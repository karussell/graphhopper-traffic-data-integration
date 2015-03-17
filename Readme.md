# GraphHopper Traffic Data Integration

Once you have traffic data you ask: how to integrate this for the routing in GraphHopper?

This project allows you to influence the routing over HTTP via posting a JSON.

It will influence routing in real time. To enable the speed mode (prepare.chWeighting=fastest)
you'll have to prepare the data again after new data arrived which will take roughly 9 minutes 
for Germany.

# Start

Start the server for your area:

 * ./td.sh datasource=your-osm-file.pbf
 * visit localhost:8989 to try routing

Now feed some data and try routing again:

```bash
curl -H "Content-Type: application/json" --data @traffic.json http://localhost:8989/datafeed
```

Note, in order to use the provided example `traffic.json` you'll have to use the specific area, get it 
[here](http://download.geofabrik.de/europe/germany/nordrhein-westfalen/duesseldorf-regbez-latest.osm.pbf)

# Data Format

The data format is very generic and can be used for other information influencing routing:

```json
[{ 
  "points": [[lon1, lat1], [lon2, lat2], ..],
  "value": 12.4,
  "type": "speed",
  "mode": "REPLACE",
}, {
   "points": ...
}
]
```

# License

This code stands under the Apache License 2.0