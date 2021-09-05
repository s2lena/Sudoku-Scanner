package com.example.sudoku;

import java.util.Vector;

public class SudokuSolver {
    public int count = 0;
    private static int[][] board = new int[9][9];
    private static final int lengthHeight = board.length;
    private static final int lengthWidth = board[0].length;

    public Vector<Integer> applyAlgorithm(Vector<Integer> predictNumbers) {
        createBoard(predictNumbers);
        solve();
        predictNumbers = createVector();
        return predictNumbers;
    }

    public void createBoard(Vector<Integer> vector) {
        int row = 0;
        int col = 0;
        for (int num: vector) {
            if (col == 9) {
                row++;
                col = 0;
            }
            board[row][col] = num;
            col++;
        }
    }

    public Vector<Integer> createVector () {
        Vector<Integer> results = new Vector<>();
        for (int i = 0; i < lengthHeight; i++) {
            for (int j = 0; j < lengthWidth; j++)
                results.add(board[i][j]);
        }
        return results;
    }

    public boolean solve() {
        int row, col;
        int[] find = findEmpty();
        if (find == null) {
            printBoard();
            return true;
        }
        else {
            row = find[0];
            col = find[1];
        }
        for (int i = 1; i < 10; i++) {
            if (valid(i, row, col)) {
                board[row][col] = i;
                if (solve())
                    return true;
                else
                    board[row][col] = 0;
            }
        }
        return false;
    }

    public boolean valid(int number, int row, int col) {
        // check row
        for (int i = 0; i < lengthHeight; i++) {
            if (board[row][i] == number && col != i)
                return false;
        }
        // check col
        for (int i = 0; i < lengthWidth; i++) {
            if (board[i][col] == number && row != i)
                return false;
        }
        // Check box
        int rowFloor = Math.floorDiv(row, 3);
        int colFloor = Math.floorDiv(col, 3);
        for (int i = rowFloor * 3; i < rowFloor * 3 + 3; i++)
            for (int j = colFloor * 3; j < colFloor * 3 + 3; j++)
                if (board[i][j] == number && row != i && col != j)
                    return false;
        return true;
    }

    public void printBoard() {
        for (int i = 0; i < lengthHeight; i++) {
            if (i % 3 == 0 && i != 0) {
                System.out.println("------------------------");
            }
            for (int j = 0; j < lengthWidth; j++) {
                if (j % 3 == 0 && j != 0)
                    System.out.print(" | ");
                if (j == 8)
                    System.out.println(board[i][j]);
                else
                    System.out.print(board[i][j]);
            }
        }
    }

    public int[] findEmpty() {
        int[] results = {0, 0};
        for (int i = 0; i < lengthHeight; i++)
            for (int j = 0; j < lengthWidth; j++)
                if (board[i][j] == 0) {
                    results[0] = i;
                    results[1] = j;
                    return results;
                }
        return null;
    }

    public boolean checkLegalSudoku(Vector<Integer> sudoku) {
        for (int num : sudoku) {
            if (num == 0)
                return false;
        }
        return true;
    }
}
