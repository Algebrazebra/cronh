package cronh.dsl

import scala.quoted.*
import cronh.domain.fieldTypes.{Hour, Minute}

object TimeMacro {

  def timeImpl(scExpr: Expr[StringContext], argsExpr: Expr[Seq[Any]])(using
      Quotes
  ): Expr[Time] = {
    import quotes.reflect.report

    (scExpr, argsExpr) match {
      // No interpolated args: the whole literal is a compile-time constant.
      case ('{ StringContext(${ Varargs(parts) }*) }, Varargs(args))
          if args.isEmpty =>
        parts match {
          case Seq(partExpr) =>
            partExpr.value match {
              case Some(literal) =>
                Time.parse(literal) match {
                  case Right(t) =>
                    // Splice a direct, already-validated construction — no runtime parse.
                    '{
                      Time(
                        Hour(${ Expr(t.hour.value) }),
                        Minute(${ Expr(t.minute.value) })
                      )
                    }
                  case Left(err) =>
                    report.errorAndAbort(
                      s"Invalid time literal: ${err.message}"
                    )
                }
              case None =>
                report.errorAndAbort(
                  "time\"...\" requires a constant string literal"
                )
            }
          case _ =>
            report.errorAndAbort("time\"...\" must be a single constant string")
        }

      // Has interpolated values (e.g. time"$hourVar:00"): unknown until runtime.
      case _ =>
        '{
          Time
            .parse($scExpr.s($argsExpr*))
            .fold(e => throw new IllegalArgumentException(e.message), identity)
        }
    }
  }
}
