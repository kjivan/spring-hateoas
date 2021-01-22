/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas.mediatype.html;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

/**
 * @author Oliver Drotbohm
 */
@Configuration
public class HtmlConfiguration implements HypermediaMappingInformation {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.config.HypermediaMappingInformation#getMediaTypes()
	 */
	@Override
	public List<MediaType> getMediaTypes() {
		return Collections.singletonList(MediaType.TEXT_HTML);
	}

	@Bean
	HttpMessageConverter<RepresentationModel<?>> htmlHttpMessageConverter(ResourceLoader loader) throws IOException {

		return new JMustacheRenderingHttpMessageConverter(loader);
	}

	private static class JMustacheRenderingHttpMessageConverter
			implements HttpMessageConverter<RepresentationModel<?>>, DisposableBean {

		private static final String TEMPLATE = "classpath:org/springframework/hateoas/mediatype/html/form.template";

		private final Template template;
		private final Reader reader;

		JMustacheRenderingHttpMessageConverter(ResourceLoader loader) throws IOException {

			this.reader = new InputStreamReader(loader.getResource(TEMPLATE).getInputStream());

			com.samskivert.mustache.Mustache.Compiler compiler = Mustache.compiler();
			this.template = compiler.compile(reader);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.http.converter.HttpMessageConverter#getSupportedMediaTypes()
		 */
		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return Collections.singletonList(MediaType.TEXT_HTML);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.http.converter.HttpMessageConverter#canRead(java.lang.Class, org.springframework.http.MediaType)
		 */
		@Override
		public boolean canRead(Class<?> clazz, MediaType mediaType) {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.http.converter.HttpMessageConverter#canWrite(java.lang.Class, org.springframework.http.MediaType)
		 */
		@Override
		public boolean canWrite(Class<?> clazz, MediaType mediaType) {
			return RepresentationModel.class.isAssignableFrom(clazz) && MediaType.TEXT_HTML.equals(mediaType);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.http.converter.HttpMessageConverter#write(java.lang.Object, org.springframework.http.MediaType, org.springframework.http.HttpOutputMessage)
		 */
		@Override
		public void write(RepresentationModel<?> t, MediaType contentType, HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {

			Affordance affordance = t.getRequiredLink(IanaLinkRelations.SELF).getAffordances().get(0);
			AffordanceModel model = affordance.getAffordanceModel(MediaType.TEXT_HTML);

			outputMessage.getBody().write(template.execute(new Object() {
				String target = model.getLink().getHref();
				Iterable<PropertyMetadata> properties = model.getInput().stream().collect(Collectors.toList());
			}).getBytes(StandardCharsets.UTF_8));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.http.converter.HttpMessageConverter#read(java.lang.Class, org.springframework.http.HttpInputMessage)
		 */
		@Override
		public RepresentationModel<?> read(Class<? extends RepresentationModel<?>> clazz, HttpInputMessage inputMessage)
				throws IOException, HttpMessageNotReadableException {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.beans.factory.DisposableBean#destroy()
		 */
		@Override
		public void destroy() throws Exception {
			this.reader.close();
		}
	}
}
