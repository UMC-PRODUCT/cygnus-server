// 이 파일은 contracts/notification/asyncapi.yaml에서 생성됩니다. 직접 수정하지 마세요.

export const notificationFcmRequestedSchema = {
  "type": "object",
  "required": [
    "schemaVersion",
    "eventId",
    "eventType",
    "source",
    "occurredAt",
    "requestId",
    "detail"
  ],
  "properties": {
    "schemaVersion": {
      "const": 1
    },
    "eventId": {
      "type": "string",
      "format": "uuid"
    },
    "eventType": {
      "type": "string",
      "const": "notification.fcm.requested.v1"
    },
    "source": {
      "type": "string",
      "minLength": 1
    },
    "occurredAt": {
      "type": "string",
      "format": "date-time"
    },
    "traceparent": {
      "type": [
        "string",
        "null"
      ],
      "maxLength": 64
    },
    "requestId": {
      "type": "string",
      "format": "uuid"
    },
    "detail": {
      "type": "object",
      "required": [
        "chunkIndex",
        "chunkCount",
        "memberIds",
        "title",
        "body",
        "data"
      ],
      "properties": {
        "chunkIndex": {
          "type": "integer",
          "minimum": 0
        },
        "chunkCount": {
          "type": "integer",
          "minimum": 1
        },
        "memberIds": {
          "type": "array",
          "maxItems": 500,
          "uniqueItems": true,
          "items": {
            "type": "integer",
            "format": "int64",
            "minimum": 1
          }
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "maxLength": 200
        },
        "body": {
          "type": "string",
          "minLength": 1,
          "maxLength": 2000
        },
        "data": {
          "type": "object",
          "additionalProperties": {
            "type": "string"
          }
        },
        "imageUrl": {
          "type": [
            "string",
            "null"
          ],
          "format": "uri"
        },
        "deepLink": {
          "type": [
            "string",
            "null"
          ],
          "maxLength": 2048
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
} as const;

export const authenticationEmailVerificationRequestedSchema = {
  "type": "object",
  "required": [
    "schemaVersion",
    "eventId",
    "eventType",
    "source",
    "occurredAt",
    "requestId",
    "detail"
  ],
  "properties": {
    "schemaVersion": {
      "const": 1
    },
    "eventId": {
      "type": "string",
      "format": "uuid"
    },
    "eventType": {
      "type": "string",
      "const": "authentication.email.verification.requested.v1"
    },
    "source": {
      "type": "string",
      "minLength": 1
    },
    "occurredAt": {
      "type": "string",
      "format": "date-time"
    },
    "traceparent": {
      "type": [
        "string",
        "null"
      ],
      "maxLength": 64
    },
    "requestId": {
      "type": "string",
      "format": "uuid"
    },
    "detail": {
      "type": "object",
      "required": [
        "email",
        "verificationCode",
        "expiresAt"
      ],
      "properties": {
        "email": {
          "type": "string",
          "format": "email",
          "maxLength": 320
        },
        "verificationCode": {
          "type": "string",
          "pattern": "^[0-9]{6}$"
        },
        "expiresAt": {
          "type": "string",
          "format": "date-time"
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
} as const;

export const recruitingInterviewEmailRequestedSchema = {
  "type": "object",
  "required": [
    "schemaVersion",
    "eventId",
    "eventType",
    "source",
    "occurredAt",
    "requestId",
    "detail"
  ],
  "properties": {
    "schemaVersion": {
      "const": 1
    },
    "eventId": {
      "type": "string",
      "format": "uuid"
    },
    "eventType": {
      "type": "string",
      "const": "recruiting.interview.email.requested.v1"
    },
    "source": {
      "type": "string",
      "minLength": 1
    },
    "occurredAt": {
      "type": "string",
      "format": "date-time"
    },
    "traceparent": {
      "type": [
        "string",
        "null"
      ],
      "maxLength": 64
    },
    "requestId": {
      "type": "string",
      "format": "uuid"
    },
    "detail": {
      "type": "object",
      "required": [
        "applicationId",
        "email",
        "applicantName",
        "availabilityFormId",
        "contactText"
      ],
      "properties": {
        "applicationId": {
          "type": "integer",
          "format": "int64",
          "minimum": 1
        },
        "email": {
          "type": "string",
          "format": "email",
          "maxLength": 320
        },
        "applicantName": {
          "type": "string",
          "minLength": 1,
          "maxLength": 100
        },
        "availabilityFormId": {
          "type": "integer",
          "format": "int64",
          "minimum": 1
        },
        "contactText": {
          "type": "string",
          "maxLength": 1000
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
} as const;

export const notificationWebhookRequestedSchema = {
  "type": "object",
  "required": [
    "schemaVersion",
    "eventId",
    "eventType",
    "source",
    "occurredAt",
    "requestId",
    "detail"
  ],
  "properties": {
    "schemaVersion": {
      "const": 1
    },
    "eventId": {
      "type": "string",
      "format": "uuid"
    },
    "eventType": {
      "type": "string",
      "const": "notification.webhook.requested.v1"
    },
    "source": {
      "type": "string",
      "minLength": 1
    },
    "occurredAt": {
      "type": "string",
      "format": "date-time"
    },
    "traceparent": {
      "type": [
        "string",
        "null"
      ],
      "maxLength": 64
    },
    "requestId": {
      "type": "string",
      "format": "uuid"
    },
    "detail": {
      "type": "object",
      "required": [
        "platforms",
        "title",
        "content",
        "attempt"
      ],
      "properties": {
        "platforms": {
          "type": "array",
          "minItems": 1,
          "uniqueItems": true,
          "items": {
            "enum": [
              "TELEGRAM",
              "DISCORD",
              "SLACK"
            ]
          }
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "maxLength": 200
        },
        "content": {
          "type": "string",
          "minLength": 1,
          "maxLength": 4000
        },
        "attempt": {
          "type": "integer",
          "minimum": 1,
          "maximum": 5
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
} as const;

export const notificationEmailResultSchema = {
  "type": "object",
  "required": [
    "schemaVersion",
    "eventId",
    "eventType",
    "source",
    "occurredAt",
    "requestId",
    "detail"
  ],
  "properties": {
    "schemaVersion": {
      "const": 1
    },
    "eventId": {
      "type": "string",
      "format": "uuid"
    },
    "eventType": {
      "type": "string",
      "minLength": 1
    },
    "source": {
      "type": "string",
      "minLength": 1
    },
    "occurredAt": {
      "type": "string",
      "format": "date-time"
    },
    "traceparent": {
      "type": [
        "string",
        "null"
      ],
      "maxLength": 64
    },
    "requestId": {
      "type": "string",
      "format": "uuid"
    },
    "detail": {
      "type": "object",
      "required": [
        "correlationType",
        "correlationId",
        "status",
        "attemptedAt"
      ],
      "properties": {
        "correlationType": {
          "enum": [
            "EMAIL_VERIFICATION",
            "RECRUITING_INTERVIEW"
          ]
        },
        "correlationId": {
          "type": "string",
          "minLength": 1,
          "maxLength": 100
        },
        "status": {
          "enum": [
            "ACCEPTED",
            "FAILED",
            "EXPIRED"
          ]
        },
        "providerMessageId": {
          "type": [
            "string",
            "null"
          ],
          "maxLength": 200
        },
        "failureCode": {
          "type": [
            "string",
            "null"
          ],
          "maxLength": 100
        },
        "attemptedAt": {
          "type": "string",
          "format": "date-time"
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
} as const;
