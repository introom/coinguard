"use strict";

const pulumi = require("@pulumi/pulumi");

const config = new pulumi.Config();

exports.projectName = `ops`;

exports.raw = config;