"use strict";

// gather outputs
global.output = {}

const config = require("./config");
const resource = require("./resource");
const cluster = require("./cluster")
const database = require("./database");
const backend = require("./backend");

require(`./${config.envName}`)

// see https://nodejs.org/api/modules.html#exports-shortcut.
module.exports = global.output
