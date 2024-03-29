package org.aks.spring.webflux.movies.client;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import lombok.extern.slf4j.Slf4j;
import org.aks.spring.webflux.movies.domain.MovieInfo;
import org.aks.spring.webflux.movies.exception.MoviesInfoClientException;
import org.aks.spring.webflux.movies.exception.MoviesInfoServerException;
import org.aks.spring.webflux.movies.util.RetryUtil;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@Slf4j
public class MoviesInfoRestClient {

    private WebClient webClient;

    @Value("${restClient.moviesInfoUrl}")
    private String moviesInfoUrl;

    public MoviesInfoRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<MovieInfo> retrieveMovieInfo(String movieId) {

        var url = moviesInfoUrl.concat("/{id}");
        /*var retrySpec = RetrySpec.fixedDelay(3, Duration.ofSeconds(1))
                .filter((ex) -> ex instanceof MoviesInfoServerException)
                .onRetryExhaustedThrow(((retryBackoffSpec, retrySignal) -> Exceptions.propagate(retrySignal.failure())));*/

        return webClient.get()
                .uri(url, movieId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (clientResponse -> {
                    log.info("Status code : {}", clientResponse.statusCode().value());
                    if (clientResponse.statusCode().equals(NOT_FOUND)) {
                        return Mono.error(new MoviesInfoClientException("There is no MovieInfo available for the passed in Id : " + movieId, clientResponse.statusCode().value()));
                    }
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(response -> Mono.error(new MoviesInfoClientException(response, clientResponse.statusCode().value())));
                }))
                .onStatus(HttpStatusCode::is5xxServerError, (clientResponse -> {
                    log.info("Status code : {}", clientResponse.statusCode().value());
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(response -> Mono.error(new MoviesInfoServerException(response)));
                }))
                .bodyToMono(MovieInfo.class)
               //.retry(3)
                //.retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500)))
                .retryWhen(RetryUtil.retrySpec())
                .log();

    }

    public Flux<MovieInfo> retrieveMovieInfoStream() {

        var url = moviesInfoUrl.concat("/stream");
        /*var retrySpec = RetrySpec.fixedDelay(3, Duration.ofSeconds(1))
                .filter((ex) -> ex instanceof MoviesInfoServerException)
                .onRetryExhaustedThrow(((retryBackoffSpec, retrySignal) -> Exceptions.propagate(retrySignal.failure())));*/

        return webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (clientResponse -> {
                    log.info("Status code : {}", clientResponse.statusCode().value());
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(response -> Mono.error(new MoviesInfoClientException(response, clientResponse.statusCode().value())));
                }))
                .onStatus(HttpStatusCode::is5xxServerError, (clientResponse -> {
                    log.info("Status code : {}", clientResponse.statusCode().value());
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(response -> Mono.error(new MoviesInfoServerException(response)));
                }))
                .bodyToFlux(MovieInfo.class)
                //.retry(3)
                .retryWhen(RetryUtil.retrySpec())
                .log();

    }

    public Mono<MovieInfo> retrieveMovieInfo_exchange(String movieId) {

        var url = moviesInfoUrl.concat("/{id}");

        return webClient.get()
                .uri(url, movieId)
                .exchangeToMono(clientResponse -> {

                    HttpStatusCode httpStatusCode = clientResponse.statusCode();
                    if (httpStatusCode.equals(OK)) {
                        return clientResponse.bodyToMono(MovieInfo.class);
                    } else if (httpStatusCode.equals(NOT_FOUND)) {
                        return Mono.error(new MoviesInfoClientException(
                            "There is no MovieInfo available for the passed in Id : " + movieId,
                            clientResponse.statusCode().value()));
                    }
                    return clientResponse.bodyToMono(String.class)
                        .flatMap(response -> Mono.error(new MoviesInfoServerException(response)));
                })
                .retryWhen(RetryUtil.retrySpec())
                .log();

    }


}

