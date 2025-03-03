/**
 * IK 中文分词  版本 5.0.1
 * IK Analyzer release 5.0.1
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
package org.wltea.analyzer.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;

/**
 * IK分词器，Lucene Analyzer接口实现
 * 兼容Lucene 4.0版本
 */
public final class IKAnalyzer extends Analyzer {

    private boolean useSmart;

    private String remoteExtDict;

    /**
     * IK分词器Lucene  Analyzer接口实现类
     * <p>
     * 默认细粒度切分算法
     */
    public IKAnalyzer() {
        this(false);
    }

    /**
     * IK分词器Lucene Analyzer接口实现类
     *
     * @param useSmart 当为true时，分词器进行智能切分
     */
    public IKAnalyzer(boolean useSmart) {
        super();
        this.useSmart = useSmart;
    }

    public IKAnalyzer(boolean useSmart, String remoteExtDict) {
        super();
        this.useSmart = useSmart;
        this.remoteExtDict = remoteExtDict;
    }

    public boolean useSmart() {
        return useSmart;
    }

    public void setUseSmart(boolean useSmart) {
        this.useSmart = useSmart;
    }

    public String getRemoteExtDict() {
        return remoteExtDict;
    }

    public void setRemoteExtDict(String remoteExtDict) {
        this.remoteExtDict = remoteExtDict;
    }

    /**
     * 重载Analyzer接口，构造分词组件
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer iktokenizer = new IKTokenizer(this.useSmart(), this.getRemoteExtDict());
        return new TokenStreamComponents(iktokenizer);
    }

}
