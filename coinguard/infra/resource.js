"use strict";

const config = require("./config");
const azure = require("@pulumi/azure-native");

exports.resourceGroup = new azure.resources.ResourceGroup(`${config.projectName}-${config.envName}`);