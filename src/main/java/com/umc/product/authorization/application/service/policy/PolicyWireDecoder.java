package com.umc.product.authorization.application.service.policy;

import com.fasterxml.jackson.databind.JsonNode;

interface PolicyWireDecoder {

    String schemaVersion();

    PolicySchema10Wire.Bundle decodeBundle(JsonNode root);

    PolicySchema10Wire.Module decodeModule(JsonNode root);
}
