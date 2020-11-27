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
package org.eclipse.emfcloud.modelserver.example;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emfcloud.modelserver.common.Routing;
import org.eclipse.emfcloud.modelserver.emf.common.JsonResponse;
import org.eclipse.emfcloud.modelserver.emf.common.ModelRepository;
import org.eclipse.emfcloud.modelserver.emf.configuration.ServerConfiguration;
import org.eclipse.emfcloud.modelserver.jsonschema.Json;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import io.javalin.Javalin;

public class CustomRouting extends Routing {
   private final Javalin javalin;

   @Inject
   public CustomRouting(final Javalin javalin, final ServerConfiguration serverConfiguration) {
      this.javalin = javalin;
   }

   @Override
   public void bindRoutes() {
      javalin.routes(() -> {
         path("api/v1", () -> {
            get("custom", ctx -> {
               Optional<String> testValue = getQueryParam(ctx.queryParamMap(), "test");
               if (testValue.isPresent()) {
                  ObjectNode data = Json.object(
                     Json.prop("test", Json.text(testValue.get())),
                     Json.prop("time", Json.text(DateTimeFormatter.ISO_LOCAL_TIME.format(LocalDateTime.now()))),
                     Json.prop("models", IntNode.valueOf(getController(ModelRepository.class).getAllModels().size())),
                     Json.prop("custom", Json.bool(true)));

                  ctx.json(JsonResponse.success(data));
               } else {
                  ctx.status(400).json(JsonResponse.error("Missing parameter 'test'!"));
               }
            });
         });
      });
   }

   private Optional<String> getQueryParam(final Map<String, List<String>> queryParams, final String paramKey) {
      return queryParams.containsKey(paramKey)
         ? Optional.of(queryParams.get(paramKey).get(0))
         : Optional.empty();
   }
}
