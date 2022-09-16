Embulk::JavaPlugin.register_filter(
  "oracle_lookup", "org.embulk.filter.oracle_lookup.OracleLookupFilterPlugin",
  File.expand_path('../../../../classpath', __FILE__))
