"use strict";

const k8s = require("@pulumi/kubernetes");
const config = require("../config");
const cluster = require("../cluster");
const util = require("../util");

const rawSettings = {
  qa: {
    storage: "10Gi",
    replicas: 2,
    host: `api.coinguard.qa.firepandalabs.com`,
    envMap: {
      "APP_ENV": config.envName,
      "APP_DB_URL": config.dbUrl,
      "APP_DB_USERNAME": config.dbUsername,
      "APP_DB_PASSWORD": config.dbPassword,
      "APP_TWILIO_ACCOUNT_SID": "AC37135f40f21df8b4a6ee296b8d665c92",
      "APP_TWILIO_AUTH_TOKEN": config.raw.requireSecret("twilioAuthToken"),
      "APP_TWILIO_FROM_ACCOUNT": "+18593507490",
    },
  },
};
const settings = rawSettings[config.envName];

const envMapName = util.resourceName`api-env-map`;
const envMap = new k8s.core.v1.Secret(
  envMapName,
  {
    metadata: {
      namespace: cluster.namespace.metadata.name,
      name: envMapName,
    },
    stringData: settings.envMap,
  },
  { provider: cluster.provider }
);

const appName = util.resourceName`api`;
const appLabels = { app: appName };

// btw, nginx-ingress does not use service to route.  it directly talks to the endpoint api.
// service is only used to associate with the pods.
const service = new k8s.core.v1.Service(
  appName,
  {
    metadata: {
      namespace: cluster.namespace.metadata.name,
      name: appName,
      labels: appLabels,
    },
    spec: {
      clusterIP: "None",
      ports: [{ port: 80, targetPort: 8080 }],
      // this is a headless service:
      // see https://kubernetes.io/docs/concepts/services-networking/service/#with-selectors
      selector: appLabels,
    },
  },
  {
    provider: cluster.provider,
    // see https://www.pulumi.com/docs/intro/concepts/resources/#customtimeouts
    // customTimeouts: { create: "3m", update: "3m" },
  }
);

const volumeName = util.resourceName`api-data`;
const statefulSet = new k8s.apps.v1.StatefulSet(
  appName,
  {
    metadata: {
      namespace: cluster.namespace.metadata.name,
      name: appName,
    },
    spec: {
      selector: {
        matchLabels: appLabels,
      },
      // the headless service this statefulset bound to.
      // we use serviceName instead of service.metadata.name is because there seems some buggy
      // behavior of circular dependency between the service and the statefulset.
      serviceName: appName,
      replicas: settings.replicas,
      template: {
        metadata: {
          labels: appLabels,
        },
        spec: {
          terminationGracePeriodSeconds: 10,
          containers: [
            {
              name: `coinguard-${config.envName}-api`,
              // change on this image is ignored
              image: "firepandalabs.azurecr.io/coinguard/backend:latest",
              imagePullPolicy: "Always",
              command: ["clojure", "-M:prod"],
              envFrom: [{ secretRef: { name: envMap.metadata.name } }],
              volumeMounts: [
                {
                  name: volumeName,
                  mountPath: "/opt/data",
                },
              ],
            },
          ],
        },
      },
      volumeClaimTemplates: [
        {
          metadata: {
            name: volumeName,
            namespace: cluster.namespace.metadata.name,
          },
          spec: {
            accessModes: ["ReadWriteOnce"],
            // see https://docs.microsoft.com/en-us/azure/aks/azure-disks-dynamic-pv#create-a-persistent-volume-claim
            storageClassName: "default",
            resources: {
              requests: {
                storage: settings.storage,
              },
            },
          },
        },
      ],
    },
  },
  {
    provider: cluster.provider,
    // https://www.pulumi.com/docs/intro/concepts/resources/options/ignorechanges/#ignorechanges
    ignoreChanges: ["spec.template.spec.containers.[0].image"]
  }
);

// ingress
const ingress = new k8s.networking.v1.Ingress(
  appName,
  {
    metadata: {
      name: appName,
      namespace: cluster.namespace.metadata.name,
      labels: appLabels,
    },
    spec: {
      // see https://kubernetes.github.io/ingress-nginx/user-guide/basic-usage/
      ingressClassName: "nginx",
      rules: [
        {
          host: settings.host,
          http: {
            paths: [
              {
                pathType: "Prefix",
                path: "/",
                backend: {
                  service: {
                    name: service.metadata.name,
                    port: { number: 80 },
                  },
                },
              },
            ],
          },
        },
      ],
    },
  },
  { provider: cluster.provider }
);
