/********************************************************************************
 * Copyright (c) 2019 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0, or the MIT License which is
 * available at https://opensource.org/licenses/MIT.
 *
 * SPDX-License-Identifier: EPL-2.0 OR MIT
 ********************************************************************************/
package org.eclipse.emfcloud.modelserver.emf.di;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emfcloud.modelserver.common.AppEntryPoint;
import org.eclipse.emfcloud.modelserver.common.EntryPointType;
import org.eclipse.emfcloud.modelserver.common.Routing;
import org.eclipse.emfcloud.modelserver.common.codecs.Codec;
import org.eclipse.emfcloud.modelserver.edit.CommandCodec;
import org.eclipse.emfcloud.modelserver.emf.common.ModelController;
import org.eclipse.emfcloud.modelserver.emf.common.ModelRepository;
import org.eclipse.emfcloud.modelserver.emf.common.ModelResourceManager;
import org.eclipse.emfcloud.modelserver.emf.common.ModelValidator;
import org.eclipse.emfcloud.modelserver.emf.common.SchemaController;
import org.eclipse.emfcloud.modelserver.emf.common.SchemaRepository;
import org.eclipse.emfcloud.modelserver.emf.common.SessionController;
import org.eclipse.emfcloud.modelserver.emf.common.codecs.Codecs;
import org.eclipse.emfcloud.modelserver.emf.common.codecs.CodecsManager;
import org.eclipse.emfcloud.modelserver.emf.configuration.CommandPackageConfiguration;
import org.eclipse.emfcloud.modelserver.emf.configuration.EPackageConfiguration;
import org.eclipse.emfcloud.modelserver.emf.configuration.EcorePackageConfiguration;
import org.eclipse.emfcloud.modelserver.emf.configuration.FacetConfig;
import org.eclipse.emfcloud.modelserver.emf.configuration.ServerConfiguration;
import org.eclipse.emfcloud.modelserver.emf.launch.ModelServerEntryPoint;
import org.eclipse.emfcloud.modelserver.emf.launch.ModelServerStartup;
import org.eclipse.emfcloud.modelserver.jsonschema.JsonSchemaConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

import io.javalin.Javalin;

public abstract class ModelServerModule extends AbstractModule {

   protected static final Logger LOG = Logger.getLogger(ModelServerModule.class.getSimpleName());

   private Multibinder<EPackageConfiguration> ePackageConfigurationBinder;
   private final ArrayList<Class<? extends EPackageConfiguration>> ePackageConfigurations;

   protected ModelServerModule() {
      ePackageConfigurations = Lists.newArrayList(
         EcorePackageConfiguration.class,
         CommandPackageConfiguration.class);
   }

   @Override
   protected void configure() {
      bind(ServerConfiguration.class).in(Singleton.class);
      ePackageConfigurationBinder = Multibinder.newSetBinder(binder(), EPackageConfiguration.class);
      ePackageConfigurations.forEach(c -> ePackageConfigurationBinder.addBinding().to(c));

      bind(ModelServerStartup.class).in(Singleton.class);
      bind(ModelController.class).in(Singleton.class);
      bind(ModelRepository.class).in(Singleton.class);
      bind(JsonSchemaConverter.class).in(Singleton.class);
      bind(SchemaController.class).in(Singleton.class);
      bind(SchemaRepository.class).in(Singleton.class);
      bind(SessionController.class).in(Singleton.class);
      MapBinder.newMapBinder(binder(), EntryPointType.class, AppEntryPoint.class).addBinding(EntryPointType.REST)
         .to(ModelServerEntryPoint.class);

      // Configure default bindings
      bind(Javalin.class).toInstance(bindJavalin());
      bind(AdapterFactory.class).toInstance(bindAdapterFactory());
      bind(CommandCodec.class).to(bindCommandCodec()).in(Singleton.class);
      bind(ModelResourceManager.class).to(bindModelResourceManager()).in(Singleton.class);
      bind(ModelValidator.class).to(bindModelValidator()).in(Singleton.class);
      bind(FacetConfig.class).to(bindFacetConfig()).in(Singleton.class);
      bind(ObjectMapper.class).toInstance(bindObjectMapper());
      bind(CodecsManager.class).to(bindCodecsManager()).in(Singleton.class);
      MapBinder<String, Codec> codecsBinder = MapBinder.newMapBinder(binder(), String.class, Codec.class);
      bindFormatCodecs().forEach((format, codec) -> codecsBinder.addBinding(format).to(codec));
      Multibinder<Routing> routingBinder = Multibinder.newSetBinder(binder(), Routing.class);
      bindModelServerRoutings().forEach(routing -> routingBinder.addBinding().to(routing).in(Singleton.class));
   }

   public void addEPackageConfigurations(final Collection<Class<? extends EPackageConfiguration>> configs) {
      ePackageConfigurations.addAll(configs);
   }

   protected Javalin bindJavalin() {
      return Javalin.create(config -> {
         config.enableCorsForAllOrigins();
         config.requestLogger((ctx, ms) -> {
            String requestPath = ctx.path() + (ctx.queryString() == null ? "" : "?" + ctx.queryString());
            LOG.info(ctx.method() + " " + requestPath + " -> Status: " + ctx.status() + " (took " + ms + " ms)");
         });
         config.asyncRequestTimeout = 5000L;
         config.wsLogger(ws -> {
            ws.onConnect(ctx -> LOG.info("WS Connected: " + ctx.getSessionId()));
            ws.onMessage(ctx -> LOG.info("WS Received: " + ctx.message() + " by " + ctx.getSessionId()));
            ws.onClose(ctx -> LOG.info("WS Closed: " + ctx.getSessionId()));
            ws.onError(ctx -> LOG.info("WS Errored: " + ctx.getSessionId()));
         });
      });
   }

   protected abstract AdapterFactory bindAdapterFactory();

   protected abstract Class<? extends CommandCodec> bindCommandCodec();

   protected abstract Class<? extends ModelResourceManager> bindModelResourceManager();

   protected abstract Class<? extends ModelValidator> bindModelValidator();

   protected abstract Class<? extends FacetConfig> bindFacetConfig();

   protected abstract ObjectMapper bindObjectMapper();

   /**
    * Bind the codecs manager implementating class.
    *
    * @return the condecs manager implementation class.
    */
   protected Class<? extends CodecsManager> bindCodecsManager() {
      return Codecs.class;
   }

   /**
    * Bind the EObject codecs to support various formats encoding and decoding.
    * <p>
    * Note that in case you don't support Json or don't want it as the default format, you may bind an alternative one
    * to the key {@link CodecsManager#PREFERRED_FORMAT}.
    * </p>
    * <p>
    * Note this binding may also no longer be relevant in case you override the {@link #bindCodecsManager()} method.
    * </>
    *
    * @return map with formats as key and codec classes to instantiate as values.
    */
   protected abstract Map<String, Class<? extends Codec>> bindFormatCodecs();

   protected abstract Set<Class<? extends Routing>> bindModelServerRoutings();

}
