# Bank Account and Money Transfers (Java)

Practical Exercise 1 - Java banking application.

## Run in VS Code
1. Install a JDK (version 17 or newer) from https://adoptium.net and restart VS Code.
2. In VS Code, install the extension "Extension Pack for Java" (by Microsoft).
3. Unzip this folder, then use File > Open Folder and pick `BankApp_VSCode`.
4. Open `src/Main.java` and click "Run" above `public static void main`.
   (Or press F5 and choose "Run Bank App (Main)".)
5. The output appears in the Terminal / Debug Console.

## Run without VS Code
- Windows: double-click `run.bat`
- Mac / Linux: `./run.sh`
- Or by hand: `javac -d bin src/*.java` then `java -cp bin Main`

## Folders
- `src/` - the Java code (Money, AccountStatus, TransactionType, BankingException, LedgerEntry, BankAccount, FundTransfer, Main)
- `docs/` - report (Word and PDF) and a sample of the program output
