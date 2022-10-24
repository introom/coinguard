"use strict";

const pulumi = require("@pulumi/pulumi");
const azure = require("@pulumi/azure-native");
const random = require("@pulumi/random");

const config = require("./config");
const resource = require("./resource");
const util = require("./util");

const rawSettings = {
  // az postgres flexible-server list-skus --location westus2 | e-
  qa: {
    version: "13",
    createMode: "Default",
    sku: {
      tier: "Burstable",
      name: "Standard_B1ms",
    },
    storageSizeGB: 128,
    backupRetentionDays: 7,
  },
  prod: {},
};
const settings = rawSettings[config.envName];

const serverBaseName = util.resourceName`database`;
// https://www.pulumi.com/registry/packages/random/api-docs/randomid/
const serverRandomId = new random.RandomId(`${serverBaseName}-random-id`, {
  byteLength: 8,
});

const serverName = pulumi.interpolate`${serverBaseName}-${serverRandomId.hex}`;
// this is the flexible server.
// the official one is too old: https://www.pulumi.com/registry/packages/azure-native/api-docs/dbforpostgresql/server/
const server = new azure.dbforpostgresql.v20210601.Server(serverName, {
  resourceGroupName: resource.resourceGroup.name,
  // set this location instead of the default `westus2` is because right atm the `westus2` has some deployment issues.
  location: "westus3",
  // NB since the serverName is random, the connection becomes more secure
  serverName: serverName,
  version: settings.version,
  createMode: settings.createMode,
  administratorLogin: "firepanda",
  // pulumi config set dbAdminPassword 'password' --secret
  administratorLoginPassword: config.raw.requireSecret("dbAdminPassword"),
  // see https://docs.microsoft.com/en-US/cli/azure/postgres/server?view=azure-cli-latest#az_postgres_server_list_skus
  // az postgres server list-skus -l westus2
  sku: settings.sku,
  storage: {
    storageSizeGB: settings.storageSizeGB,
  },
  backup: {
      backupRetentionDays: settings.backupRetentionDays,
      geoRedundantBackup: "Disabled",
  },
});

// env specific
switch (config.envName) {
  case "qa":
    const fwName = util.resourceName`allow-all`;
    const firewallRule = new azure.dbforpostgresql.v20210601.FirewallRule(fwName, {
      firewallRuleName: fwName,
      startIpAddress: "0.0.0.0",
      endIpAddress: "255.255.255.255",
      resourceGroupName: resource.resourceGroup.name,
      serverName: server.name,
    });
    break;
}