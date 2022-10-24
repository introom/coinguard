"use strict";

const azure = require("@pulumi/azure-native");
// const azure_classic = require("@pulumi/azure");
// const azuread = require("@pulumi/azuread");
const k8s = require("@pulumi/kubernetes");

const config = require("./config");
// const resources = require("./resources");
const cluster = require("./cluster");
const utils = require("./utils");

const appName = utils.resourceName`app`;

// see https://github.com/kubernetes/ingress-nginx/blob/main/charts/ingress-nginx/values.yaml
const nginx = new k8s.helm.v3.Chart(
  nginxName,
  {
    namespace: cluster.namespace.metadata.name,
    chart: "ingress-nginx",
    version: "4.0.13",
    fetchOpts: { repo: "https://kubernetes.github.io/ingress-nginx" },
    values: {
      controller: {
        admissionWebhooks: { enabled: false },
        service: {
          loadBalancerIP: publicIPAddress.ipAddress,
          annotations: {
            // we can access the load balancer with an azure domain: http://coinguard-qa.westus2.cloudapp.azure.com/
            "service.beta.kubernetes.io/azure-dns-label-name": `${config.projectName}-${config.envName}`,
          },
          publishService: { enabled: true },
        },
        ingressClassResource: {
          name: nginxName,
        },
      },
    },
    transformations: [
      (obj) => {
        // NB do transformations on the yaml to set all resources to the namespace
        if (obj.metadata) {
          obj.metadata.namespace = cluster.namespace.metadata.name;
        }
      },
    ],
  },
  { provider: cluster.provider }
);
