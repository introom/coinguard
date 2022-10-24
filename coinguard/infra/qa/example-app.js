"use strict";

const k8s = require("@pulumi/kubernetes");

const util = require("../util");
const cluster = require("../cluster");

const appName = util.resourceName`example-hello`;
const appLabels = { app: appName };

const deployment = new k8s.apps.v1.Deployment(
  appName,
  {
    metadata: { namespace: cluster.namespace.metadata.name, labels: appLabels },
    spec: {
      replicas: 1,
      selector: { matchLabels: appLabels },
      template: {
        metadata: { labels: appLabels },
        spec: {
          containers: [
            {
              name: appName,
              image: "mcr.microsoft.com/azuredocs/aks-helloworld:v1",
              resources: { requests: { cpu: "50m", memory: "20Mi" } },
              ports: [{ name: "http", containerPort: 80 }],
              env: [{ name: "TITLE", value: "Welcome to Firepanda Labs!" }],
            },
          ],
        },
      },
    },
  },
  { provider: cluster.provider }
);

const service = new k8s.core.v1.Service(
  appName,
  {
    metadata: {
      namespace: cluster.namespace.metadata.name,
      name: appName,
      labels: appLabels,
    },
    spec: { ports: [{ port: 80, targetPort: 80 }], selector: appLabels },
  },
  { provider: cluster.provider }
);

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
          host: 'hello.coinguard.qa.firepandalabs.com',
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
