# GraphHopper Traffic Data Integration

This project allows you to influence the routing via posting a JSON over HTTP or fetching the info
from custom data source of your choice, e.g. Cologne:

![traffic info preview](https://karussell.files.wordpress.com/2015/04/ghmaps-with-traffic.png)

The nice thing is that the routing will change immediately after posting or fetching the data, i.e. in real time.

When using the speed mode (prepare.chWeighting=fastest) real time is not possible for large areas as you'll have to 
prepare the data again after new data arrived which will take roughly 9 minutes for Germany. So only near-real-time.

# Start

Start the server for your area:

 * ./td.sh datasource=your-osm-file.pbf
 * visit http://localhost:8989 to try routing in our UI

Now feed some data and try routing again:

```bash
curl -H "Content-Type: application/json" --data @traffic.json http://localhost:8989/datafeed
```

Note, in order to use the provided example `traffic.json` you'll have to use the specific area, get it 
[here](http://download.geofabrik.de/europe/germany/nordrhein-westfalen/duesseldorf-regbez-latest.osm.pbf)

![Traffic influenced routing](./traffic.gif)

# Data Format

The data format is very generic and can be used for other information influencing routing:

```json
[{
   "id": "1",
   "points": [[6.827273, 51.190264]],
   "value": 10,
   "value_type": "speed",
   "mode": "REPLACE"
}, {
   "id": "somethingelse",
   "points": ...
}
]
```

Note, the point list is in geo json and therefor use lon,lat instead of the more common lat,lon order

# License

This code stands under the Apache License 2.0