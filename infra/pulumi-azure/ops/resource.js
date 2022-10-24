"use strict";

const config = require("./config");
const azure = require("@pulumi/azure-native");

// `get` because we manually created the resource group in the portal
// NB this is the subscription id of the `default` subscription
const resourceGroupId = "/subscriptions/561c1f3f-a5c2-4ba4-924d-6c02b11df6fd/resourceGroups/ops"
exports.resourceGroup = azure.resources.ResourceGroup.get(config.projectName, resourceGroupId);