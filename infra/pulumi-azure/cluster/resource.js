"use strict";

const azure = require("@pulumi/azure-native");

const config = require("./config");

exports.resourceGroup = new azure.resources.ResourceGroup(`${config.projectName}-${config.envName}`);