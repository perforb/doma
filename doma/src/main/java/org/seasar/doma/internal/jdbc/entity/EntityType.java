/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.doma.internal.jdbc.entity;

import java.util.List;
import java.util.Map;

import org.seasar.doma.jdbc.entity.NamingConvention;
import org.seasar.doma.wrapper.Wrapper;

/**
 * エンティティのインスタンスを管理するクラスを表します。
 * 
 * <p>
 * このインタフェースの実装はスレッドセーフであることは要求されません。
 * </p>
 * 
 * @author taedium
 * 
 * @param <E>
 *            エンティティの型
 */
public interface EntityType<E> {

    String getName();

    String getCatalogName();

    String getSchemaName();

    String getTableName();

    GeneratedIdPropertyType<?> getGeneratedIdPropertyType();

    VersionPropertyType<?> getVersionPropertyType();

    EntityPropertyType<?> getEntityPropertyType(String __name);

    List<EntityPropertyType<?>> getEntityPropertyTypes();

    E getEntity();

    Class<E> getEntityClass();

    Map<String, Wrapper<?>> getOriginalStates();

    NamingConvention getNamingConvention();

    void refreshEntity();

    void preInsert();

    void preUpdate();

    void preDelete();
}
