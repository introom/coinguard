"use strict";

const config = require("./config");

function resourceName(strings, ...rest) {
    let s = String.raw({raw: strings}, ...rest)
    // no env name is added for ops
    return `${config.projectName}-${s}`; 
}
exports.resourceName = resourceName;