"use strict";

const pulumi = require("@pulumi/pulumi");

const config = new pulumi.Config();

exports.projectName = `coinguard`;

exports.envName = config.require("envName");

// cat path/to/kubeconfig | pulumi config set kubeconfig --secret    
exports.kubeconfig = config.require("kubeconfig");

exports.raw = config;

exports.dbUrl = config.require("dbUrl")
exports.dbUsername = "firepanda"
exports.dbPassword = config.requireSecret("dbPassword")