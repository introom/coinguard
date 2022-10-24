"use strict";

const config = require("./config");

function resourceName(strings, ...rest) {
    let s = String.raw({raw: strings}, ...rest)
    return `${config.projectName}-${config.envName}-${s}`; 
}
exports.resourceName = resourceName;