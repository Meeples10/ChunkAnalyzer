# ChunkAnalyzer

For more information, see [this page](https://meeples10.github.io/resource-distribution.html).

This plugin was tested with [Paper build 364](https://papermc.io/downloads) (v1.0.0) and [Paper build 661](https://papermc.io/downloads) (v1.1.0).

Note that this plugin is very unoptimized, and as such is **not** recommended for use on a normal server. For larger worlds, the plugin requires that a significant amount of memory be allocated to the server process. If the server can handle it, increasing the `view-distance` in `server.properties` and decreasing the `teleport-delay` in the plugin's `config.yml` can help to speed up the data collection.

After installing the plugin use the command `/chunkanalyzer` (or `/ca`) to begin teleporting in a spiral out from (0, 0). This serves as a simple way to automatically generate and load chunks, which the plugin then analyzes.
