"use strict";

const pulumi = require("@pulumi/pulumi");
const tls = require("@pulumi/tls");
const azure = require("@pulumi/azure-native");
const azure_classic = require("@pulumi/azure");
const k8s = require("@pulumi/kubernetes");

const config = require("./config");
const util = require("./util");
const resource = require("./resource");

const rawSettings = {
  qa: {
    clusterMaxCount: 5,
    clusterOnDiskSize: 60,
    clusterVmSize: "Standard_D2s_v5",
  },
};

const settings = rawSettings[config.envName];

const clusterName = util.resourceName`k8s`;

const sshKey = new tls.PrivateKey(clusterName, {
  algorithm: "RSA",
  rsaBits: 4096,
});

// node resource group must be different from resourceGroup.name
const nodeResourceGroupName = util.resourceName`k8s-node`;
exports.nodeResourceGroupName = nodeResourceGroupName;

const cluster = new azure.containerservice.ManagedCluster(clusterName, {
  resourceGroupName: resource.resourceGroup.name,
  agentPoolProfiles: [
    {
      // - is not allowed in the name
      name: `cluster${config.envName}`,
      mode: "System",
      count: 1,
      enableAutoScaling: true,
      minCount: 1,
      maxCount: settings.clusterMaxCount,
      osType: "Linux",
      osDiskSizeGB: settings.clusterOnDiskSize,
      type: "VirtualMachineScaleSets",
      vmSize: settings.clusterVmSize,
    },
  ],
  dnsPrefix: clusterName,
  enableRBAC: true,
  // az aks get-versions --location westus2 --output table
  kubernetesVersion: "1.23.5",
  // there is no latest version.
  // kubernetesVersion: "latest",
  linuxProfile: {
    adminUsername: "firepanda",
    ssh: {
      publicKeys: [
        {
          keyData: sshKey.publicKeyOpenssh,
        },
      ],
    },
  },
  nodeResourceGroup: nodeResourceGroupName,
  // system managed identity
  identity: {
    type: "SystemAssigned",
  },
});
exports.cluster = cluster;

// acr permission
const subscriptionId = azure.authorization.getClientConfig().then(c => c.subscriptionId)
const registryId = pulumi.interpolate`/subscriptions/${subscriptionId}/resourceGroups/ops/providers/Microsoft.ContainerRegistry/registries/firepandalabs`
const principalId = cluster.identityProfile.apply(p => p["kubeletidentity"].objectId)
const acrAuth = new azure_classic.authorization.Assignment(util.resourceName`k8s-acr-auth`, {
  scope: registryId,
  roleDefinitionName: "acrpull",
  principalId: principalId,
});

// for export usage
const creds = azure.containerservice.listManagedClusterUserCredentialsOutput({
  resourceGroupName: resource.resourceGroup.name,
  resourceName: cluster.name,
});
const kubeconfig = creds.kubeconfigs[0].value.apply((enc) =>
  Buffer.from(enc, "base64").toString()
);

// NB get the kubeconfig
// global.output.kubeconfig = kubeconfig;
// pulumi stack output kubeconfig > dev-local/qa/kubeconfig

const namespaceName = "infra";

const provider = new k8s.Provider("k8s", {
  kubeconfig: kubeconfig,
  // default namespace
  // https://www.pulumi.com/registry/packages/kubernetes/api-docs/provider/
  namespace: namespaceName,
  
  // we use helm release for helm charts having hooks
  // suppressHelmHookWarnings: true,
});
exports.provider = provider;

exports.namespace = new k8s.core.v1.Namespace(
  namespaceName,
  {
    metadata: { name: namespaceName },
  },
  { provider: provider }
);
