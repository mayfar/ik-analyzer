/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 */
package org.wltea.analyzer.dic;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wltea.analyzer.cfg.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 词典管理类,单子模式
 */
public class Dictionary {

    private static final Logger LOG = LoggerFactory.getLogger(Dictionary.class);

    /**
     * 词典单子实例
     */
    private static Dictionary singleton;

    /**
     * 主词典对象
     */
    private DictSegment mainDict;

    /**
     * 停止词词典
     */
    private DictSegment stopWordDict;
    /**
     * 量词词典
     */
    private DictSegment quantifierDict;

    /**
     * 配置对象
     */
    private Configuration cfg;

    private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

    private Dictionary(Configuration cfg) {
        this.cfg = cfg;
        this.loadMainDict();
        this.loadStopWordDict();
        this.loadQuantifierDict();
    }

    /**
     * 词典初始化
     * 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
     * 只有当Dictionary类被实际调用时，才会开始载入词典，
     * 这将延长首次分词操作的时间
     * 该方法提供了一个在应用加载阶段就初始化字典的手段
     *
     * @return Dictionary
     */
    public static Dictionary initial(Configuration cfg) {
        if (singleton == null) {
            synchronized(Dictionary.class) {
                if (singleton == null) {
                    singleton = new Dictionary(cfg);
                    singleton.loadMainDict();
                    singleton.loadQuantifierDict();
                    singleton.loadStopWordDict();

                    // 建立监控线程
                    if(cfg.getRemoteExtDict() != null){
                        pool.scheduleAtFixedRate(new Monitor(cfg.getRemoteExtDict()), 0, 60, TimeUnit.SECONDS);
                    }
                    return singleton;
                }
            }
        }
        return singleton;
    }

    /**
     * 获取词典单子实例
     *
     * @return Dictionary 单例对象
     */
    public static Dictionary getSingleton() {
        if (singleton == null) {
            throw new IllegalStateException("词典尚未初始化，请先调用initial方法");
        }
        return singleton;
    }

    /**
     * 批量加载新词条
     *
     * @param words Collection<String>词条列表
     */
    public void addWords(Collection<String> words) {
        if (words != null) {
            for (String word : words) {
                if (word != null) {
                    //批量加载词条到主内存词典中
                    singleton.mainDict.fillSegment(word.trim().toLowerCase().toCharArray());
                }
            }
        }
    }

    /**
     * 批量移除（屏蔽）词条
     *
     * @param words
     */
    public void disableWords(Collection<String> words) {
        if (words != null) {
            for (String word : words) {
                if (word != null) {
                    //批量屏蔽词条
                    singleton.mainDict.disableSegment(word.trim().toLowerCase().toCharArray());
                }
            }
        }
    }

    /**
     * 检索匹配主词典
     *
     * @param charArray
     *
     * @return Hit 匹配结果描述
     */
    public Hit matchInMainDict(char[] charArray) {
        return singleton.mainDict.match(charArray);
    }

    /**
     * 检索匹配主词典
     *
     * @param charArray
     * @param begin
     * @param length
     *
     * @return Hit 匹配结果描述
     */
    public Hit matchInMainDict(char[] charArray, int begin, int length) {
        return singleton.mainDict.match(charArray, begin, length);
    }

    /**
     * 检索匹配量词词典
     *
     * @param charArray
     * @param begin
     * @param length
     *
     * @return Hit 匹配结果描述
     */
    public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
        return singleton.quantifierDict.match(charArray, begin, length);
    }

    /**
     * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
     *
     * @param charArray
     * @param currentIndex
     * @param matchedHit
     *
     * @return Hit
     */
    public Hit matchWithHit(char[] charArray, int currentIndex, Hit matchedHit) {
        DictSegment ds = matchedHit.getMatchedDictSegment();
        return ds.match(charArray, currentIndex, 1, matchedHit);
    }

    /**
     * 判断是否是停止词
     *
     * @param charArray
     * @param begin
     * @param length
     *
     * @return boolean
     */
    public boolean isStopWord(char[] charArray, int begin, int length) {
        return singleton.stopWordDict.match(charArray, begin, length).isMatch();
    }

    /**
     * 加载主词典及扩展词典
     */
    private void loadMainDict() {
        //建立一个主词典实例
        mainDict = new DictSegment((char) 0);
        //读取主词典文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(cfg.getMainDictionary());
        if (is == null) {
            throw new RuntimeException("Main Dictionary not found!!!");
        }

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
            String theWord = null;
            do {
                theWord = br.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    mainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                }
            } while (theWord != null);

        } catch (IOException ioe) {
            LOG.error("Main Dictionary loading exception.", ioe);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                LOG.error("io error.", e);
            }
        }
        //加载扩展词典
        this.loadExtDict();
        //加载远程自定义词典
        this.loadRemoteExtDict();
    }

    /**
     * 加载用户配置的扩展词典到主词库表
     */
    private void loadExtDict() {
        //加载扩展词典配置
        List<String> extDictFiles = cfg.getExtDictionarys();
        if (extDictFiles != null) {
            InputStream is;
            for (String extDictName : extDictFiles) {
                //读取扩展词典文件
                LOG.info("加载扩展词典:{}", extDictName);
                is = this.getClass().getClassLoader().getResourceAsStream(extDictName);
                //如果找不到扩展的字典，则忽略
                if (is == null) {
                    continue;
                }
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
                    String theWord = null;
                    do {
                        theWord = br.readLine();
                        if (theWord != null && !"".equals(theWord.trim())) {
                            //加载扩展词典数据到主内存词典中
                            mainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                        }
                    } while (theWord != null);

                } catch (IOException ioe) {
                    LOG.error("Extension Dictionary loading exception.", ioe);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        LOG.error("io error.", e);
                    }
                }
            }
        }
    }

    /**
     * 加载用户扩展的停止词词典
     */
    private void loadStopWordDict() {
        //建立一个主词典实例
        stopWordDict = new DictSegment((char) 0);
        //加载扩展停止词典
        List<String> extStopWordDictFiles = cfg.getExtStopWordDictionarys();
        if (extStopWordDictFiles != null) {
            InputStream is = null;
            for (String extStopWordDictName : extStopWordDictFiles) {
                LOG.info("加载扩展停止词典:{}", extStopWordDictName);
                //读取扩展词典文件
                is = this.getClass().getClassLoader().getResourceAsStream(extStopWordDictName);
                //如果找不到扩展的字典，则忽略
                if (is == null) {
                    continue;
                }
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
                    String theWord = null;
                    do {
                        theWord = br.readLine();
                        if (theWord != null && !"".equals(theWord.trim())) {
                            //加载扩展停止词典数据到内存中
                            stopWordDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                        }
                    } while (theWord != null);

                } catch (IOException ioe) {
                    LOG.error("Extension Stop word Dictionary loading exception.", ioe);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        LOG.error("io error.", e);
                    }
                }
            }
        }

    }

    /**
     * 加载量词词典
     */
    private void loadQuantifierDict() {
        //建立一个量词典实例
        quantifierDict = new DictSegment((char) 0);
        //读取量词词典文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(cfg.getQuantifierDicionary());
        if (is == null) {
            throw new RuntimeException("Quantifier Dictionary not found!!!");
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
            String theWord;
            do {
                theWord = br.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    quantifierDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                }
            } while (theWord != null);

        } catch (IOException ioe) {
            LOG.error("Quantifier Dictionary loading exception.", ioe);

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                LOG.error("io error.", e);
            }
        }
    }

    /**
     * 加载远程扩展词典到主词库表
     */
    private void loadRemoteExtDict() {
        String location = cfg.getRemoteExtDict();
        if(location == null){
            return;
        }
        LOG.info("加载远程扩展词典：" + location);
        List<String> lists = getRemoteWords(location);
        // 如果找不到扩展的字典，则忽略
        if (lists == null) {
            return;
        }
        for (String theWord : lists) {
            if (theWord != null && !"".equals(theWord.trim())) {
                // 加载扩展词典数据到主内存词典中
                mainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
            }
        }
        LOG.info("加载远程扩展词典size：" + lists.size());
    }

    /**
     * 从远程服务器上下载自定义词条
     */
    private static List<String> getRemoteWords(String location) {

        List<String> buffer = new ArrayList<String>();
        RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000).setConnectTimeout(10 * 1000)
                .setSocketTimeout(60 * 1000).build();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;
        BufferedReader in;
        HttpGet get = new HttpGet(location);
        get.setConfig(rc);
        try {
            response = httpclient.execute(get);
            if (response.getStatusLine().getStatusCode() == 200 && response.getEntity() != null) {
                String charset = "UTF-8";
                // 获取编码，默认为utf-8
                if (response.getEntity().getContentType() != null && response.getEntity().getContentType().getValue().contains("charset=")) {
                    String contentType = response.getEntity().getContentType().getValue();
                    charset = contentType.substring(contentType.lastIndexOf("=") + 1);
                }
                in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
                String line;
                while ((line = in.readLine()) != null) {
                    buffer.add(line);
                }
                in.close();
                response.close();
                return buffer;
            }
            response.close();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }


    /**
     * 重新加载词库
     *
     */
    public void reloadMainDict() {
        LOG.info("开始重新加载远程词库...");
        // 新开一个实例加载词典，减少加载过程对当前词典使用的影响
        Dictionary tmpDict = new Dictionary(cfg);
        mainDict = tmpDict.mainDict;
    }

}
