# #76 Integrated ddns with approach similar to skibish/ddns

- State: open
- Author: @IRus
- Created: 2021-05-29T11:03:30Z
- Updated: 2021-05-29T11:03:30Z
- Closed: none
- Labels: none
- Assignees: @IRus
- Milestone: none
- Comments: 0
- URL: https://github.com/Heapy/komok/issues/76

## Body

https://github.com/skibish/ddns

```
# DDNS configuration file.

# Mandatory, DigitalOcean API token.
# It can be also set using environment variable DDNS_TOKEN.
token: ""

# By default, IP check occurs every 5 minutes.
# It can be also set using environment variable DDNS_CHECKPERIOD.
checkPeriod: "5m"

# By default, timeout to external resources is set to 10 seconds.
# It can be also set using environment variable DDNS_REQUESTTIMEOUT.
requestTimeout: "10s"

# By default, IPv6 address is not requested.
# IPv6 address can be forced by setting it to `true`.
# It can be also set using environment variable DDNS_IPV6.
ipv6: false

# List of domains and their records to update.
domains:
  example.com:
  # More details about the fields can be found here:
  # https://developers.digitalocean.com/documentation/v2/#create-a-new-domain-record
  - type: "A"
    name: "www"
  - type: "TXT"
    name: "demo"

    # By default, is set to "{{.IP}}" (key .IP is reserved).
    # Supports Go template engine.
    # Additional keys can be set in "params" block below.
    data: "My IP is {{.IP}} and I am {{.mood}}"

    # By default, 1800 seconds (5 minutes).
    ttl: 1800
```

## Comments (0)

_No comments._
