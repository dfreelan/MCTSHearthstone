package behaviors.util;

@FunctionalInterface
public interface IArrayCompressor<T>
{
    T compress(T[] array);
}