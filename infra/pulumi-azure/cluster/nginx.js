"use strict";

const pulumi = require("@pulumi/pulumi");
const azure = require("@pulumi/azure-native");
const k8s = require("@pulumi/kubernetes");

const config = require("./config");
const cluster = require("./cluster");
const util = require("./util");

// static ip for nginx
const nginxName = util.resourceName`nginx`;
// NB this ip shall NOT belong to the coinguard resource group
const publicIPAddress = new azure.network.PublicIPAddress(
  nginxName,
  {
    // we must allocate ip to the node resource group so that nginx can use it
    resourceGroupName: cluster.nodeResourceGroupName,
    publicIpAddressName: nginxName,
    publicIPAllocationMethod: "Static",
    sku: {
      name: "Standard",
    },
  },
  { dependsOn: [cluster.cluster] }
);
exports.publicIPAddress = publicIPAddress;

// see https://www.pulumi.com/registry/packages/kubernetes/api-docs/helm/v3/release/
const defaultCertName = util.resourceName`default-cert`;
exports.defaultCertName = defaultCertName;

const nginx = new k8s.helm.v3.Release(
  nginxName,
  {
    namespace: cluster.namespace.metadata.name,
    chart: "ingress-nginx",
    // see https://github.com/kubernetes/ingress-nginx/tree/main/charts/ingress-nginx
    // see https://github.com/kubernetes/ingress-nginx/blob/main/charts/ingress-nginx/values.yaml
    repositoryOpts: { repo: "https://kubernetes.github.io/ingress-nginx" },
    version: "4.1.1",
    values: {
      controller: {
        service: {
          loadBalancerIP: publicIPAddress.ipAddress,
          // see https://kubernetes.github.io/ingress-nginx/how-it-works/#avoiding-outage-from-wrong-configuration
          admissionWebhooks: { enabled: true },
          annotations: {
            // we can access the load balancer with an azure domain: http://fpl-cluster-qa.westus2.cloudapp.azure.com/
            "service.beta.kubernetes.io/azure-dns-label-name": `fpl-${config.projectName}-${config.envName}`,
          },
        },
        ingressClassResource: {
          // this name will be referenced in the ingressClassName field of the ingress resource
          name: "nginx",
        },
        extraArgs: {
          "default-ssl-certificate": pulumi.interpolate`${cluster.namespace.metadata.name}/${defaultCertName}`,
        },
      },
    },
  },
  { provider: cluster.provider }
);

exports.ingressClassName = nginxName;
