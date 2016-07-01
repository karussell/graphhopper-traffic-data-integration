var host = "https://graphhopper.com/api/1";
var key = "016f1b38-62f0-4a2b-88f7-dc5b743a9b56";

exports.options = {
    environment: "production",
    routing: {host: host, api_key: key},
    geocoding: {host: host, api_key: key}
};