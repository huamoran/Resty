package cn.dreampie.orm.cache;


import cn.dreampie.cache.CacheEvent;
import cn.dreampie.cache.CacheProvider;
import cn.dreampie.common.Constant;
import cn.dreampie.common.util.Joiner;
import cn.dreampie.log.Logger;

import java.util.Arrays;


/**
 * This is a main cache facade. It could be architected in the future to add more cache implementations besides OSCache.
 */
public enum QueryCache {
  INSTANCE;

  private final static Logger logger = Logger.getLogger(QueryCache.class);

  private final boolean enabled = Constant.cacheEnabled;

  private final CacheProvider cacheProvider;

  //singleton

  private QueryCache() {
    cacheProvider = CacheProvider.PROVIDER;
  }


  /**
   * This class is a singleton, get an instance with this method.
   *
   * @return one and only one instance of this class.
   */
  public static QueryCache instance() {
    return INSTANCE;
  }

  static void logAccess(String group, String query, Object[] params, String access) {
    if (logger.isDebugEnabled()) {
      StringBuilder log = new StringBuilder().append(access).append(", group: {").append(group).append("}, query: {").append(query).append("} ");
      if (params != null && params.length > 0) {
        log.append(", params: ").append('{');
        log.append(Joiner.on("}, {").useForNull("null").join(params));
        log.append('}');
      }
      logger.debug(log.toString());
    }
  }

  /**
   * Adds an item to cache. Expected some lists of objects returned from "select" queries.
   *
   * @param tableName - name of table.
   * @param query     query text
   * @param params    - list of parameters for a query.
   * @param cache     object to cache.
   */
  public void add(String type, String dsName, String tableName, String query, Object[] params, Object cache) {
    if (enabled) {
      String group = getGroup(type, dsName, tableName);
      cacheProvider.addCache(group, getKey(group, query, params), cache);
    }
  }

  private String getGroup(String type, String dsName, String tableName) {
    return type + Constant.CONNECTOR + dsName + Constant.CONNECTOR + tableName;
  }

  /**
   * Returns an item from cache, or null if nothing found.
   *
   * @param tableName name of table.
   * @param query     query text.
   * @param params    list of query parameters, can be null if no parameters are provided.
   * @return cache object or null if nothing found.
   */
  public <T> T get(String type, String dsName, String tableName, String query, Object[] params) {

    if (enabled) {
      String group = getGroup(type, dsName, tableName);
      String key = getKey(group, query, params);
      Object item = cacheProvider.getCache(group, key);
      if (item == null) {
        logAccess(group, query, params, "Miss");
      } else {
        logAccess(group, query, params, "Hit");
        return (T) item;
      }
    }
    return null;
  }

  private String getKey(String group, String query, Object[] params) {
    return group + Constant.CONNECTOR + query + Constant.CONNECTOR + (params == null ? null : Arrays.asList(params).toString());
  }

  private String getKey(String type, String dsName, String tableName, String query, Object[] params) {
    return getKey(getGroup(type, dsName, tableName), query, params);
  }

  public void remove(String type, String dsName, String tableName, String query, Object[] params) {
    if (enabled) {
      String group = getGroup(type, dsName, tableName);
      cacheProvider.removeCache(group, getKey(group, query, params));
    }
  }

  /**
   * This method purges (removes) all caches associated with a table, if caching is enabled and
   * a corresponding model is marked cached.
   *
   * @param tableName table name whose caches are to be purged.
   */
  public void purge(String type, String dsName, String tableName) {
    if (enabled) {
      cacheProvider.flush(new CacheEvent(getGroup(type, dsName, tableName), getClass().getName()));
    }
  }

  public CacheProvider getCacheProvider() {
    return cacheProvider;
  }
}
