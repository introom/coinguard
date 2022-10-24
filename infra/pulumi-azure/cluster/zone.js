"use strict";

const azure = require("@pulumi/azure-native");

const config = require("./config");
const resource = require("./resource");
const util = require("./util");
const nginx = require("./nginx");

// NB should be ONLY something like: {qa|prod}.firepandalabs.com
const zoneName = `${config.envName}.firepandalabs.com`;
exports.zoneName = zoneName;
const zone = new azure.network.Zone(zoneName, {
  resourceGroupName: resource.resourceGroup.name,
  // azure zone is a global resource
  location: "Global",
  zoneName: zoneName,
});
exports.zone = zone;

// add nginx static ip to zone
new azure.network.RecordSet(util.resourceName`root-record-set`, {
  recordType: "A",
  aRecords: [
    {
      ipv4Address: nginx.publicIPAddress.ipAddress,
    },
  ],
  // @ matches the root domain
  relativeRecordSetName: "@",
  resourceGroupName: resource.resourceGroup.name,
  ttl: 60,
  zoneName: zone.name,
});

new azure.network.RecordSet(util.resourceName`default-record-set`, {
  recordType: "A",
  aRecords: [
    {
      ipv4Address: nginx.publicIPAddress.ipAddress,
    },
  ],
  // * wild card matches all
  relativeRecordSetName: "*",
  resourceGroupName: resource.resourceGroup.name,
  ttl: 60,
  zoneName: zone.name,
});


// XXX project specific
// coinguard
new azure.network.RecordSet(util.resourceName`coinguard-default-record-set`, {
  recordType: "A",
  aRecords: [
    {
      ipv4Address: nginx.publicIPAddress.ipAddress,
    },
  ],
  // * wild card matches all
  relativeRecordSetName: "*.coinguard",
  resourceGroupName: resource.resourceGroup.name,
  ttl: 60,
  zoneName: zone.name,
});