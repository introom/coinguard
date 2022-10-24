"use strict";

// gather outputs
global.output = {}

// the modules here are imported in an order satisfying the partial ordering of different modules.
const config = require("./config");
const resource = require("./resource")
const cluster = require("./cluster")
const nginx = require("./nginx")
const zone = require("./zone")
const cert = require("./cert")

require(`./${config.envName}`)

// see https://nodejs.org/api/modules.html#exports-shortcut.
module.exports = global.output