"use strict";

const azure = require("@pulumi/azure-native");

const util = require("./util");
const resource = require("./resource");

const settings = {
  accountName: "fplstate",
};

const clusterStateName = "cluster-state";
const clusterState = new azure.storage.BlobContainer(
  util.resourceName`${clusterStateName}`,
  {
    accountName: settings.accountName,
    containerName: clusterStateName,
    resourceGroupName: resource.resourceGroup.name,
  }
);

// project: coinguard
const coinguardStateName = "coinguard-state";
const coinguardState = new azure.storage.BlobContainer(
  util.resourceName`${coinguardStateName}`,
  {
    accountName: settings.accountName,
    containerName: coinguardStateName,
    resourceGroupName: resource.resourceGroup.name,
  }
);

// personal-matthew
const personalMatthewStateName = "personal-matthew-state";
const personalMatthewState = new azure.storage.BlobContainer(
  util.resourceName`${personalMatthewStateName}`,
  {
    accountName: settings.accountName,
    containerName: personalMatthewStateName,
    resourceGroupName: resource.resourceGroup.name,
  }
);
