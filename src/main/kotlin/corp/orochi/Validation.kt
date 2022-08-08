package corp.orochi

sealed class Validation<out A> {
  data class Success<out T>(val value: T) : Validation<T>()
  data class Failure<out T>(val error: String) : Validation<T>()

  inline infix fun <reified B> map(
    func: (A) -> B
  ): Validation<B> = when (this) {
    is Failure -> Failure(error)
    is Success -> try {
      Success(func(value))
    } catch (ex: Exception) {
      Failure("$value -> ${B::class.java}:\n\n$ex")
    }
  }

  infix fun <B> flatMap(func: (A) -> Validation<B>): Validation<B> = map(func).flatten()

  companion object {
    fun <A> all(validations: List<Validation<A>>): Validation<List<A>> = try {
      Success(
        validations.map {
          it as? Success ?: throw UnwrapException(it as Failure)
        }.map { it.value }
      )
    } catch (ex: UnwrapException) {
      Failure(ex.causeValidation.error)
    }

    fun <A> all(vararg validations: Validation<A>) =
      all(validations.toList())

    fun <A> all(validations: Set<Validation<A>>): Validation<Set<A>> =
      all(validations.toList()) map { it.toSet() }

    class UnwrapException(val causeValidation: Failure<*>) : Exception(causeValidation.error)

    fun <A> wrap(throwingValidation: () -> A): Validation<A> = try {
      Success(throwingValidation())
    } catch (ex: ValidationException) {
      Failure(ex.localizedMessage)
    }

    fun <A> flatWrap(throwingValidation: () -> Validation<A>): Validation<A> = try {
      Success(throwingValidation()).flatten()
    } catch (ex: ValidationException) {
      Failure(ex.localizedMessage)
    }
  }
}

fun <A> Validation<Validation<A>>.flatten(): Validation<A> = when (this) {
  is Validation.Failure -> Validation.Failure(error)
  is Validation.Success -> when (value) {
    is Validation.Failure -> Validation.Failure(value.error)
    is Validation.Success -> Validation.Success(value.value)
  }
}

fun Any.invalidValue(label: String) = "Invalid value for $label: '$this'"

fun Any.invalidListValue(label: String) = "Invalid value found in $label: '$this'"

fun Any.unparseableValue(label: String) = "Failed to parse value for $label: '$this'"

class ValidationException(message: String, cause: Exception? = null) : Exception(message, cause)
