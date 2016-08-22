# Build Json filter plugin for Embulk

Build JSON object with JSON template.

## Overview

* **Plugin type**: filter

## Configuration

- **column**: description (integer, required)
  - **name**: The name of output column_name (string, default: `"json_payload"`)
  - **type**: Column type (`json` or `string`, default: `json`)
  - **template**: JSON template (The `!column_name`` parameter replace column value)

## Example

```yaml
filters:
  - type: build_json
    column:
      name: json_payload
      type: json
      template: '{"long": [!id,!account,"test"], "name":!comment}'
```

## Build

```
$ ./gradlew gem  # -t to watch change of files and rebuild continuously
```
