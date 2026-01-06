package net.shiroha233.roadweaverpg.common.result;

import net.shiroha233.roadweaverpg.common.exception.QuestException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 操作结果封装类
 * 用于替代直接抛出异常，提供更优雅的错误处理
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {
    
    boolean isSuccess();
    
    default boolean isFailure() { return !isSuccess(); }
    
    Optional<T> getValue();
    
    Optional<QuestException.ErrorCode> getErrorCode();
    
    Optional<String> getErrorMessage();
    
    Result<T> onSuccess(Consumer<T> action);
    
    Result<T> onFailure(Consumer<Failure<T>> action);
    
    <R> Result<R> map(Function<T, R> mapper);
    
    T getOrElse(T defaultValue);
    
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }
    
    static <T> Result<T> failure(QuestException.ErrorCode code, String message) {
        return new Failure<>(code, message);
    }
    
    static <T> Result<T> failure(QuestException ex) {
        return new Failure<>(ex.getErrorCode(), ex.getMessage());
    }
    
    record Success<T>(T value) implements Result<T> {
        @Override public boolean isSuccess() { return true; }
        @Override public Optional<T> getValue() { return Optional.ofNullable(value); }
        @Override public Optional<QuestException.ErrorCode> getErrorCode() { return Optional.empty(); }
        @Override public Optional<String> getErrorMessage() { return Optional.empty(); }
        
        @Override
        public Result<T> onSuccess(Consumer<T> action) {
            action.accept(value);
            return this;
        }
        
        @Override
        public Result<T> onFailure(Consumer<Failure<T>> action) { return this; }
        
        @Override
        public <R> Result<R> map(Function<T, R> mapper) {
            return new Success<>(mapper.apply(value));
        }
        
        @Override
        public T getOrElse(T defaultValue) { return value != null ? value : defaultValue; }
    }
    
    record Failure<T>(QuestException.ErrorCode errorCode, String message) implements Result<T> {
        @Override public boolean isSuccess() { return false; }
        @Override public Optional<T> getValue() { return Optional.empty(); }
        @Override public Optional<QuestException.ErrorCode> getErrorCode() { return Optional.of(errorCode); }
        @Override public Optional<String> getErrorMessage() { return Optional.of(message); }
        
        @Override
        public Result<T> onSuccess(Consumer<T> action) { return this; }
        
        @Override
        public Result<T> onFailure(Consumer<Failure<T>> action) {
            action.accept(this);
            return this;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> map(Function<T, R> mapper) {
            return (Result<R>) this;
        }
        
        @Override
        public T getOrElse(T defaultValue) { return defaultValue; }
    }
}
