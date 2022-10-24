"use strict";

const azure = require("@pulumi/azure-native");
const k8s = require("@pulumi/kubernetes");

const config = require("./config");

const namespaceName = "coinguard";

const provider = new k8s.Provider("k8s", {
  kubeconfig: config.kubeconfig,
  // default namespace
  // https://www.pulumi.com/registry/packages/kubernetes/api-docs/provider/
  namespace: namespaceName,
  suppressHelmHookWarnings: true,
});
exports.provider = provider;

exports.namespace = new k8s.core.v1.Namespace(
  namespaceName,
  {
    metadata: { name: namespaceName },
  },
  { provider: provider }
);

provider.namespace = namespaceName;
