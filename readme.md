魔改版 lox 编程语言。类似于 javascript。

* 大部分语句需要以`;`结尾。
* 支持`//`和`/**/`注释

## 构建

* `make file`：编译，然后运行`test.lox`文件
* `make repl`：编译，然后运行 repl 模式。
* `make jar`：构建 `myjlox.jar`。
    * `java -jar myjlox.jar` 来运行 repl 模式。
    * `java -jar myjlox.jar file.lox`：来运行一个 lox 文件。

## 字面量

* 数字。不区分 `int`/`float`。所有数字本质上都是 `double`。数字支持加减乘除。
* `true`/`false`
* `nil`：类似于 `null`
* 字符串。字符串可以和其他字面量用`+`拼接。比如`"hello " + "huhu"`
* `f-string`：类似于 python 中的 `f-string`，以`f`开头，内部可以用`{}`来求值。`f "my name is {name}"`.

## print关键字

输出到 `stdout`。

`print "hello world!"`

## 变量

`var` 关键字申明变量。作为动态类型语言，不需要标注类型。

```lox
var name = "hhuh";
```

变量仅在当前作用域中生效。一个作用域中同名的变量只能申明一次。

## 函数

`fun` 关键字申明函数。如果没有返回值，默认返回 `nil`

```lox
fun hey(greeting, who) {
	print f "{greeting} to {who}";
}
```

函数内部可以嵌套地申明其他函数、类。

函数使用闭包（closure）。函数可以作为返回值、参数。

## if/else/while/for

和 c/java/js 保持一致。没有特殊之处。目前还不支持 `break`, `continue` 关键字。

## with in循环

类似于 python 中的 `for in` 循环。

```lox
with num in (1, 2, 3) {
	print num;
}
```

要求`in`后面的对象具有`iter`函数，且这个 iter 函数需要返回一个具有`hasNext`和`next`两个函数的对象。

内建的 `Array` 和 `String` 默认支持。自定义类也可以通过实现 `iter` 函数来使用 `with in`循环。

## 数组

`[n]`会产生一个长度为`n`的数组。其中每个值默认都是 nil。`length()`返回数组的长度。

```lox
var arr = [5]; 
print arr.length(); // 5
arr[0] = "anda";
```

***

`(a, b, c)`产生一个刚好包括`a, b, c`三个元素的数组。

```lox
var arr = (1, "anda", 4);
print arr.length(); // 3
```

***

拆包。在赋值时，左侧可以是一个`()`形式的数组，那么会将右侧的数组中的每个元素按顺序赋值给左侧的元素。

```lox
var (a, b, c) = (1, 2, 3);
```

# class

构造函数用`init`表示。类可以作为返回值、参数。

在 `class` 内部，**如果要访问自己的属性或者方法，必须使用`this`。**作为动态语言，类似于 python 和 js，我们可以随意地为一个对象添加属性。

内部可以申明函数（此时省略 `fun` 关键字）

```lox
class Animal {
	init(name) { 
		this.name = name;
	}
	run() {
		print f "the animla {this.name} is running!";
	}
}
```

在创建对象的时候，**不需要使用`new`**。直接调用类名即可。

```lox
var dog = Animal("huhu");
dog.run();
```

## 静态属性

`class` 内部还可以有静态变量/函数。它们属于这个 `class`，而非某个具体的对象

```lox
class Math {
	static PI = 3.1415926;
	static getArea(radius) {
		return 2 * PI * radius;
	}
}
```

## 继承

申明 `class` 的时候，用`:`可以标注父类的名字。一个类只可以有一个直接父类。

`super`关键字可以用来访问父类中的方法。

```lox
class Dog: Animal {
	init(name, age) {
		super.init(name);
		this.age = age;
	}
}
```

如果一个类没有写明父类，那么它的父类是 `Origin`，类似于 java 中的 `Object`。它有下列属性

* `has(field)`：返回自己是否具有某个属性。比如`dog.has("name")`
* `is(type)`：返回自己是不是某个类的对象，比如`dog.is(Dog)`。

## import

`import` 可以执行并导入另一个 lox 文件。它的格式是这样的：

```lox
import "huhu";	// 导入整个文件，储存在一个叫做 huhu 的对象中

import "./huhu/resources/hellas" as h; // 导入整个文件，储存在一个叫做 h 的对象中。如果不改名，默认为 hellas。

import "huhu": run, sleep; // 导入 run 和 sleep 两个属性。

import "huhu": run as r, sleep; // 导入 run，改名为 r；导入 sleep；
```

导入后便可以调用。

```lox
import "huhu";
huhu.run();
```

`import` 可以在任何地方使用，并不必须是文件的最顶层。

## native

一些用 java 写的内建函数，可以完成用单纯的 lox 无法完成的一些功能。

* `native.clock()`：返回某个可以代表运行时间的数字。
* `native.panic(message)`：终止程序。参数为附带的提示消息。
* `native.len(string | array)`：返回字符串或者数组的长度。`string.length()`和`array.length()`实际上就是调用这个函数。
* `native.charAt(string, index)`：返回某个字符串在某个索引上的字符。`string.charAt(index)`实际上就是调用这个。
* `native.type(value)`：返回某个值的的类型的字符串表达。
* `native.has(object, field)`：返回 obejct 是否具有 field（一个字符串）。`Origin.has(field)`实际上就是调用这个。
* `native.is(value, class)`：返回 value是不是 class 的对象。`Origin.is(type)`实际上就是调用这个。

## 标准库

一个非常简易的标准库，这些库和普通的库没有任何区别，只不过会默认导入。

### range

类似于 python 中的 range，实现了`iter`函数，用于快速循环特定次数。

```lox
with i in range(10) {
	print i;
}
```

### enum

类似于 python 中的 enumerate，每次迭代额外返回一个索引

```lox
var arr = ("anda", "lus", "huhuh");
with (i, name) in enum(arr) {
	print f "{i}: {name}";
}
```

### List

类似于 java 中的 ArrayList。

* `init(capacity)`：创建一个具有指定初始容量的列表
* `List.from(iterable)`：静态函数。根据一个 iterable 对象，返回一个新的数组，包含那个 iterable 对象中的每一个元素。
    * `var list = List.from( (1, 2, 3) )`
* `get(index)`：获取指定索引的元素
* `add(item)`：将一个新的元素添加到末尾
* `addToIndex(index, item)`：将一个新的元素添加到指定索引
* `size`：代表 size 的属性（而不是函数）
* `iter()`：支持`with in`循环

大约就是如此，连`remove`方法都没有。

