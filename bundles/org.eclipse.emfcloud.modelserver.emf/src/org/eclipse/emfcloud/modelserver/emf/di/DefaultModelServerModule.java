/********************************************************************************
 * Copyright (c) 2020 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0, or the MIT License which is
 * available at https://opensource.org/licenses/MIT.
 *
 * SPDX-License-Identifier: EPL-2.0 OR MIT
 ********************************************************************************/
package org.eclipse.emfcloud.modelserver.emf.di;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emfcloud.modelserver.common.ModelServerPathParameters;
import org.eclipse.emfcloud.modelserver.common.Routing;
import org.eclipse.emfcloud.modelserver.common.codecs.Codec;
import org.eclipse.emfcloud.modelserver.common.codecs.XmiCodec;
import org.eclipse.emfcloud.modelserver.edit.CommandCodec;
import org.eclipse.emfcloud.modelserver.edit.DefaultCommandCodec;
import org.eclipse.emfcloud.modelserver.emf.common.DefaultFacetConfig;
import org.eclipse.emfcloud.modelserver.emf.common.DefaultModelResourceManager;
import org.eclipse.emfcloud.modelserver.emf.common.DefaultModelValidator;
import org.eclipse.emfcloud.modelserver.emf.common.ModelResourceManager;
import org.eclipse.emfcloud.modelserver.emf.common.ModelValidator;
import org.eclipse.emfcloud.modelserver.emf.common.ModelServerRouting;
import org.eclipse.emfcloud.modelserver.emf.common.codecs.JsonCodec;
import org.eclipse.emfcloud.modelserver.emf.configuration.FacetConfig;
import org.emfjson.jackson.module.EMFModule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public class DefaultModelServerModule extends ModelServerModule {

   @Override
   protected AdapterFactory bindAdapterFactory() {
      return new ComposedAdapterFactory();
   }

   @Override
   protected Class<? extends CommandCodec> bindCommandCodec() {
      return DefaultCommandCodec.class;
   }

   @Override
   protected Class<? extends ModelResourceManager> bindModelResourceManager() {
      return DefaultModelResourceManager.class;
   }

   @Override
   protected Class<? extends ModelValidator> bindModelValidator() {
      return DefaultModelValidator.class;
   }

   @Override
   protected Class<? extends FacetConfig> bindFacetConfig() {
      return DefaultFacetConfig.class;
   }

   @Override
   protected ObjectMapper bindObjectMapper() {
      return EMFModule.setupDefaultMapper();
   }

   @Override
   protected Map<String, Class<? extends Codec>> bindFormatCodecs() {
      Map<String, Class<? extends Codec>> codecs = Maps.newHashMapWithExpectedSize(2);
      codecs.put(ModelServerPathParameters.FORMAT_XMI, XmiCodec.class);
      codecs.put(ModelServerPathParameters.FORMAT_JSON, JsonCodec.class);
      return codecs;
   }

   @Override
   protected Set<Class<? extends Routing>> bindModelServerRoutings() {
      Set<Class<? extends Routing>> routings = new HashSet<>();
      routings.add(ModelServerRouting.class);
      return routings;
   }

}
