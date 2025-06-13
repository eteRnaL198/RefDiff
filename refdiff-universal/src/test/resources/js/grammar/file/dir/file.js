// 1. 変数宣言 (var, let, const)
var globalVar = "これはグローバル変数です。";
let blockScopedLet = 100;
const constantValue = true;

// 2. データ型 (プリミティブ型とオブジェクト型)
let stringType = "Hello, World!";
let numberType = 123.45;
let booleanType = false;
let nullType = null;
let undefinedType = undefined;
let symbolType = Symbol('unique');
let bigIntType = 9007199254740991n; // BigInt

let objectType = {
    key1: "value1",
    key2: 200,
    nestedObject: {
        nestedKey: "nestedValue"
    }
};

let arrayType = [1, "two", true, null, { prop: "value" }];

// 3. 演算子
let sum = 10 + 20;
let difference = 30 - 5;
let product = 6 * 7;
let quotient = 49 / 7;
let remainder = 10 % 3;
let exponentiation = 2 ** 3; // ES7

let logicalAnd = true && false;
let logicalOr = true || false;
let logicalNot = !true;

let comparison = (sum > difference) && (product <= quotient);
let strictEquality = (10 === "10");
let looseEquality = (10 == "10");

let ternaryOperator = (sum > 0) ? "Positive" : "Negative";

// 4. 制御フロー (if/else, switch, for, while, do/while)
if (blockScopedLet > 50) {
    console.log("blockScopedLetは50より大きいです。");
} else if (blockScopedLet === 100) {
    console.log("blockScopedLetは100です。");
} else {
    console.log("blockScopedLetは50以下です。");
}

switch (stringType) {
    case "Hello, World!":
        console.log("文字列が一致しました。");
        break;
    case "Goodbye":
        console.log("別の文字列です。");
        break;
    default:
        console.log("どの文字列にも一致しませんでした。");
}

for (let i = 0; i < arrayType.length; i++) {
    console.log(`配列要素 ${i}: ${arrayType[i]}`);
}

for (const prop in objectType) {
    console.log(`オブジェクトプロパティ ${prop}: ${objectType[prop]}`);
}

for (const value of arrayType) { // ES6
    console.log(`配列の各要素: ${value}`);
}

let count = 0;
while (count < 3) {
    console.log(`whileループ: ${count}`);
    count++;
}

let doWhileCount = 0;
do {
    console.log(`do/whileループ: ${doWhileCount}`);
    doWhileCount++;
} while (doWhileCount < 2);

// 5. 関数宣言 (file_level_testsではシンプルなもののみ)
function simpleFileFunction(param) {
    return `ファイルレベル関数: ${param}`;
}
console.log(simpleFileFunction("テスト"));


// 6. オブジェクトリテラル拡張 (ES6)
const propName = "dynamicProp";
const extendedObject = {
    method() { // メソッド定義省略記法
        console.log("拡張オブジェクトのメソッド");
    },
    [propName]: "動的なプロパティ", // 計算されたプロパティ名
    constantValue // プロパティ値省略記法
};
extendedObject.method();
console.log(extendedObject.dynamicProp);
console.log(extendedObject.constantValue);

// 7. 分割代入 (ES6)
const [first, second, ...rest] = arrayType;
console.log(`分割代入: ${first}, ${second}, ${rest}`);

const { key1, nestedObject: { nestedKey } } = objectType;
console.log(`オブジェクト分割代入: ${key1}, ${nestedKey}`);

// 8. スプレッド構文 (ES6)
const newArray = [...arrayType, 4, 5];
console.log(`スプレッド構文配列: ${newArray}`);

const newObject = { ...objectType, newProp: "新しいプロパティ" };
console.log(`スプレッド構文オブジェクト:`, newObject);

// 9. テンプレートリテラル (ES6)
const templateString = `合計は ${sum} で、積は ${product} です。`;
console.log(templateString);

// 10. モジュール (import/export) - 構文解析器はこれらを認識する必要がある
// export const moduleVariable = "これはエクスポートされた変数です。";
// export function moduleFunction() {
//     console.log("エクスポートされた関数です。");
// }
// export class ModuleClass {
//     constructor() {
//         console.log("エクスポートされたクラスです。");
//     }
// }
// import { moduleVariable, moduleFunction, ModuleClass } from './anotherModule.js';

// 11. 非同期処理 (Promise, async/await) - トップレベルawaitはモジュールスコープでのみ有効だが、構文自体は解析可能
async function handleFileAsyncOperations() {
    try {
        const result = await new Promise(resolve => setTimeout(() => resolve("ファイルレベル非同期成功！"), 50));
        console.log(`ファイルレベル非同期結果: ${result}`);
    } catch (error) {
        console.error(`ファイルレベル非同期エラー: ${error}`);
    }
}
handleFileAsyncOperations();

// 12. エラーハンドリング (try/catch/finally, throw)
function mightThrowFileError(shouldThrow) {
    if (shouldThrow) {
        throw new Error("ファイルレベルで意図的なエラー！");
    }
    return "ファイルレベルでエラーなし。";
}

try {
    console.log(mightThrowFileError(false));
    // console.log(mightThrowFileError(true)); // エラーテストのためコメントアウト
} catch (e) {
    console.error(`ファイルレベルでキャッチされたエラー: ${e.message}`);
} finally {
    console.log("ファイルレベルtry/catch/finallyブロックが終了しました。");
}

// 13. Map と Set (ES6)
const fileMap = new Map();
fileMap.set('id', 123);
console.log(`ファイルレベルMapのサイズ: ${fileMap.size}`);

const fileSet = new Set();
fileSet.add('item1');
console.log(`ファイルレベルSetのサイズ: ${fileSet.size}`);

// 14. WeakMap と WeakSet (ES6)
const fileWeakMapKey = {};
const fileWeakMap = new WeakMap();
fileWeakMap.set(fileWeakMapKey, "ファイルレベルWeakMapの値");

const fileWeakSetObj = {};
const fileWeakSet = new WeakSet();
fileWeakSet.add(fileWeakSetObj);

// 15. Proxy と Reflect (ES6)
const fileTarget = { a: 1 };
const fileHandler = { get: (t, p) => Reflect.get(t, p) * 10 };
const fileProxy = new Proxy(fileTarget, fileHandler);
console.log(`ファイルレベルProxy: ${fileProxy.a}`);

// 16. シンボル (ES6)
const FILE_SYMBOL = Symbol('fileSymbol');
const fileObjWithSymbol = {
    [FILE_SYMBOL]: "ファイルレベルシンボル"
};
console.log(fileObjWithSymbol[FILE_SYMBOL]);

// 17. Nullish coalescing operator (??) (ES2020)
const fileNullVal = null;
const fileResult = fileNullVal ?? "ファイルレベルデフォルト";
console.log(`ファイルレベルNullish coalescing: ${fileResult}`);

// 18. Optional chaining (?.) (ES2020)
const fileConfig = { settings: { theme: "dark" } };
console.log(fileConfig?.settings?.theme);

// 19. import() 動的インポート (ES2020) - 構文としてはサポートするべきだが、ランタイムで評価される
// if (false) { // 実行されないようにfalseに設定
//     import('./dynamic_module.js')
//         .then(module => {
//             module.runDynamic();
//         })
//         .catch(err => {
//             console.error("ファイルレベル動的インポートエラー:", err);
//         });
// }

// 20. トップレベルawait (ES2022) - モジュールとして実行される場合のみ有効。スクリプトではエラーになる可能性あり。
// (async () => {
//     const fileFetchedData = await Promise.resolve("ファイルレベルでフェッチされたデータ");
//     console.log('ファイルレベルFetched data:', fileFetchedData);
// })();

// 21. Regexp match indices (ES2022)
const fileRegex = /(abc)(def)/d;
const fileStr = 'abcdef';
const fileMatch = fileStr.match(fileRegex);
if (fileMatch) {
    console.log(`ファイルレベルRegExp match indices:`, fileMatch.indices);
}

// 22. Array.prototype.at() (ES2022)
const fileArr = [1, 2, 3];
console.log(`ファイルレベルArray.at(-1): ${fileArr.at(-1)}`);

// 23. Object.hasOwn() (ES2022)
const fileObj = { a: 1 };
console.log(`ファイルレベルObject.hasOwn('a'): ${Object.hasOwn(fileObj, 'a')}`);

// 24. IIFE (即時実行関数式)
(function() {
    console.log("これはファイルレベルの即時実行関数です。");
})();