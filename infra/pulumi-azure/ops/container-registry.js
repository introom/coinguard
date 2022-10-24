"use strict";

const azure = require("@pulumi/azure-native");

const util = require("./util");
const resource = require("./resource");

exports.registry = new azure.containerregistry.Registry(
  util.resourceName`container-registry`,
  {
    adminUserEnabled: true,
    registryName: "firepandalabs",
    resourceGroupName: resource.resourceGroup.name,
    sku: {
      name: "Standard",
    },
  }
);