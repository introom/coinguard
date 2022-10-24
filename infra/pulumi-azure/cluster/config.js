"use strict";

const pulumi = require("@pulumi/pulumi");

const config = new pulumi.Config();

exports.projectName = "cluster";
exports.envName = config.require("envName");

exports.raw = config;