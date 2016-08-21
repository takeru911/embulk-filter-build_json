Embulk::JavaPlugin.register_filter(
  "build_json", "org.embulk.filter.build_json.BuildJsonFilterPlugin",
  File.expand_path('../../../../classpath', __FILE__))
