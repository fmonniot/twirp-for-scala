package printers

import scalapb.compiler.{DescriptorPimps, FunctionalPrinter}

trait Printer extends DescriptorPimps {

  def print(printer: FunctionalPrinter): FunctionalPrinter

}
