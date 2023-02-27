package ca.bc.gov.educ.api.distribution.util;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import ca.bc.gov.educ.api.distribution.model.dto.BaseModel;
import ca.bc.gov.educ.api.distribution.model.dto.ProcessorData;
import ca.bc.gov.educ.api.distribution.model.dto.ResponseObj;
import ca.bc.gov.educ.api.distribution.model.dto.ResponseObjCache;
import io.github.resilience4j.retry.annotation.Retry;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Utility class used to construct {@link ResponseEntity} for various HTTP methods.
 */
@Component
public class RestUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestUtils.class);

	private ModelMapper modelMapper;
	private ResponseObjCache responseObjCache;
	private final EducDistributionApiConstants constants;
	private final WebClient webClient;

	@Autowired
	public RestUtils(ModelMapper modelMapper, ResponseObjCache responseObjCache, EducDistributionApiConstants constants, WebClient webClient) {
		this.modelMapper = modelMapper;
		this.responseObjCache = responseObjCache;
		this.constants = constants;
		this.webClient = webClient;
	}

	public ResponseObj getTokenResponseObject() {
		if(responseObjCache.isExpired()){
			responseObjCache.setResponseObj(getResponseObj());
		}
		return responseObjCache.getResponseObj();
	}
	public String fetchAccessToken() {
		return this.getTokenResponseObject().getAccess_token();
	}

	public void fetchAccessToken(ProcessorData data) {
		LOGGER.debug("Fetching the access token from KeyCloak API");
		ResponseObj res = getTokenResponseObject();
		if (res != null) {
			data.setAccessToken(res.getAccess_token());
			LOGGER.debug("Setting the new access token in summaryDTO.");
		}
	}

	public String getAccessToken() {
		return this.fetchAccessToken();
	}

	@Retry(name = "rt-getToken", fallbackMethod = "rtGetTokenFallback")
	private ResponseObj getResponseObj() {
		LOGGER.debug("Fetch token");
		HttpHeaders httpHeadersKC = EducDistributionApiUtils.getHeaders(
				constants.getUserName(), constants.getPassword());
		MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
		map.add("grant_type", "client_credentials");
		return this.webClient.post().uri(constants.getTokenUrl())
				.headers(h -> h.addAll(httpHeadersKC))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(map))
				.retrieve()
				.bodyToMono(ResponseObj.class).block();
	}

	public ResponseObj rtGetTokenFallBack(HttpServerErrorException exception){
		LOGGER.error("{} NOT REACHABLE after many attempts.", constants.getTokenUrl(), exception);
		return null;
	}

	public ResponseEntity<Void> FORBIDDEN() {
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	public <T> ResponseEntity<T> NOT_FOUND() {
		return new ResponseEntity<T>(HttpStatus.NOT_FOUND);
	}

	//************   GET methods

	/**
	 * Get Response Entity using JPA Entity Source
	 * @param <T>
	 * @param optional - the JPA entity Source
	 * @param type - The API model type to map to.
	 * @return
	 */
	protected <T> ResponseEntity<T> GET(Optional<?> optional, Class<T> type) {
		if (optional.isPresent()) {
			T model = modelMapper.map(optional.get(), type);
			if (model instanceof BaseModel) {
				return ResponseEntity.
						ok().						
						body(model);
			} else {
				return new ResponseEntity<>(model, HttpStatus.OK);
			}
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);

	}

	/**
	 * Get Response Entity using a LIST of JPA Entity Sources
	 * @param <T>
	 * @return
	 */
	public <T> ResponseEntity<List<T>> GET(Collection<?> entitySet, Type T) {
		if (entitySet == null || entitySet.isEmpty()) {
			return new ResponseEntity<List<T>>(new LinkedList<T>(), HttpStatus.OK);
		} else {
			List<T> list = modelMapper.map(Arrays.asList(entitySet.toArray()), T);
			return new ResponseEntity<List<T>>(list, HttpStatus.OK);
		}
	}
	
	/**
	 * Get Response Entity using a LIST of REST Model objects. This
	 * should only be used when you need fine-grained control over the model
	 * mapping and the mapping gets performed within the API.
	 * @param <T>
	 * @return
	 */
	public <T> ResponseEntity<List<T>> GET(List<T> list) {
		return new ResponseEntity<List<T>>(list, HttpStatus.OK);
	}

	/**
	 * Get Response entity directly from API model value
	 * @param <T>
	 * @param value
	 * @return
	 */
	public <T> ResponseEntity<T> GET(T value) {
		if (value == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		if (value instanceof BaseModel) {
			return ResponseEntity.
					ok().
					body(value);
		} else {
			return new ResponseEntity<>(value, HttpStatus.OK);
		}
	}

	
	//************   CREATE methods	
	public <T> ResponseEntity<ApiResponseModel<T>> CREATED(Object returnValue, Class<T> type) {
		return new ResponseEntity<>(ApiResponseModel.SUCCESS(modelMapper.map(returnValue, type)), HttpStatus.OK);
	}
	
	protected <T> ResponseEntity<ApiResponseModel<T>> CREATED(Object returnValue, List<String> warningMessages, Class<T> type) {
		
		return new ResponseEntity<>(ApiResponseModel.WARNING(modelMapper.map(returnValue, type), warningMessages), HttpStatus.MULTI_STATUS);
	}

	public <T> ResponseEntity<ApiResponseModel<T>> CREATED(T returnValue) {
		return new ResponseEntity<>(ApiResponseModel.SUCCESS(returnValue), HttpStatus.OK);
	}	
	
	//************   Updated methods	
	public <T> ResponseEntity<ApiResponseModel<T>> UPDATED(Object returnValue, Class<T> type) {
		if (returnValue == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(ApiResponseModel.SUCCESS(modelMapper.map(returnValue, type)), HttpStatus.OK);
	}

	public <T> ResponseEntity<ApiResponseModel<T>> UPDATED(T returnValue) {
		if (returnValue == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(ApiResponseModel.SUCCESS(returnValue), HttpStatus.OK);
	}
	
	protected ResponseEntity<ApiResponseModel<Void>> UPDATED() {
		return new ResponseEntity<>(ApiResponseModel.SUCCESS(null), HttpStatus.OK);
	}



	//************   DELETE methods
	public <T> ResponseEntity<Void> DELETE(int deleteCount) {
		if (deleteCount == 0) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
	
	protected <T> ResponseEntity<Void> DELETE() {
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	protected <T> ResponseEntity<ApiResponseModel<T>> ACCEPTED(Object returnValue, Class<T> type) {
		if (returnValue == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(ApiResponseModel.SUCCESS(modelMapper.map(returnValue, type)), HttpStatus.ACCEPTED);
	}
	
	/**
	 * Add converters to be used in model mapping.
	 * @param converters
	 */
	protected void addConverters(Converter<?, ?>... converters) {
		Stream.of(converters).forEach((converter) -> {
			modelMapper.addConverter(converter);
		});
	}

}
