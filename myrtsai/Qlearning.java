/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myrtsai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.Math.random;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author msi-
 */
public class Qlearning {
    //属性：
    int actionNum;
    int stateNum;
    String QMatrixFileName;
    String rewardFileName;
    File QMatrixFile = null;
    File rewardFile=null;
    double alpha;          //学习率
    double r;    //远见率
    double rewardMatrix[][]=null;
    double QMatrix[][]=null;    //Q矩阵
    int actionLast;        //上一步的决策 即 a_n-1
    int stateLast;         //上一步的状态 即 s_n-1
 //方法
    /*
    构造函数
    input: a:action 数目
           s:state数目
           a1: alpha的值
           r1：r的值
           QFile： 文件名，从文件中，读取Q矩阵
           rFile:     文件名，从文件中读取reward矩阵
           
    */
    Qlearning(int a,int s,double a1,double r1, String QFile,String rFile){
        //数据的初始化
        actionNum=a; stateNum=s; alpha=a1; r=r1; 
        QMatrixFileName=QFile;
        rewardFileName =  rFile;
        rewardMatrix=new double[actionNum][stateNum];
        QMatrix= new double[actionNum][stateNum];
        actionLast=-1;
        stateLast=-1;
        try{
            QMatrixFile = new File(QMatrixFileName);
            rewardFile = new File(rewardFileName);
            //检查文件是否存在,如果不存在新建文件，并且对两个矩阵的数据进行初始化
            if(!QMatrixFile.exists()&&!rewardFile.exists()){
                  System.out.println("创建数据");
                  QMatrixFile.createNewFile();
                  rewardFile.createNewFile();
                  //初始化Q和R矩阵
                  int i,j;
                  for(i=0;i<actionNum;i++){
                      for(j=0;j<stateNum;j++){
                          QMatrix[i][j] = 0;
                      }
                  }
                  //电脑写reward
                  //这里是按章50个state 进行赋值，具体情况要有所改动
                  for(i=0;i<actionNum;i++){
                      for(j=0;j<stateNum;j++){
                         switch(j/10){
                             case 0: rewardMatrix[i][j] = -100;break;
                             case 1: rewardMatrix[i][j] = -50;break;
                             case 2: rewardMatrix[i][j] = 0;break;
                             case 3: rewardMatrix[i][j] = 50;break;
                             case 4: rewardMatrix[i][j] =100;break;
                             default: rewardMatrix[i][j] = 0;
                         }
                      }
                  }
                  for(j=0;j<stateNum;j++){
                       rewardMatrix[1][j]=100;
                       rewardMatrix[5][j]=100;
                  }
                  //写入数据
                  writeToFile(true);
            }else{
                //从文件中读取数据  
                System.out.println("读取数据");
                readFromFile();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    //文件读取和写入的函数
    private void readFromFile(){
        //采用字符流
        try{
            //创建流对象
            InputStreamReader QReader = new InputStreamReader(new FileInputStream(QMatrixFile));
            InputStreamReader R_Reader= new InputStreamReader(new FileInputStream(rewardFile));
            BufferedReader QBuffer = new BufferedReader(QReader);
            BufferedReader R_Buffer= new BufferedReader(R_Reader);
            //开始读数据
            String Qline="";   
            String Rline="";
            String Q_DoubleString[] = new String[actionNum]; //用来存放Q数据的字符串数组
            String R_DoubleString[] = new String[actionNum]; //用来存放R数据的字符串数组
            //一行一行地读数据
            int i;
            for(i = 0;i<actionNum;i++){
                Qline=QBuffer.readLine();
                Rline = R_Buffer.readLine();
                if(Qline!=null && Rline!=null){
                    //将数据切割（文本文件中数据按照空格隔开
                    Q_DoubleString = Qline.split(" ");
                    R_DoubleString = Rline.split(" ");
                    //赋值
                    int j;
                    for(j=0;j<stateNum;j++){
                        QMatrix[i][j]=Double.parseDouble(Q_DoubleString[j]);
                        rewardMatrix[i][j]= Double.parseDouble(R_DoubleString[j]);
                    }
                }
            }
            //关闭流
            QBuffer.close();
            R_Buffer.close();
             QReader.close();
             R_Reader.close();
        }catch(Exception e){
            e.printStackTrace();
        }
 
    }
    
    //input： bool 是否写reward矩阵
    private void writeToFile(boolean isWriteReward){
      //保留两位有效小鼠
     DecimalFormat f = new DecimalFormat("#.00");
      System.out.println("开始写Q矩阵");
      try{
          BufferedWriter QWriter = new BufferedWriter(new FileWriter(QMatrixFile));
          //开始写Q矩阵
          int i,j;
          for(i=0;i<actionNum;i++){
              for(j=0;j<stateNum;j++){
                  //写Q
                  QWriter.write( f.format(QMatrix[i][j]));
                  QWriter.write(" ");
              }
              //换行
              QWriter.newLine();
          }
          //将流压入文件
          QWriter.flush();
          //关闭流
          QWriter.close();
          //开始写R矩阵
          if(isWriteReward){
             BufferedWriter RWriter = new BufferedWriter(new FileWriter(rewardFile));
             for(i=0;i<actionNum;i++){
                for(j=0;j<stateNum;j++){
                    //写R
                    RWriter.write(f.format(rewardMatrix[i][j]));
                    RWriter.write(" ");
                }
                //换行
                RWriter.newLine();
              }
              //将流压入文件
              RWriter.flush();
               //关闭流
              RWriter.close();
          }  
      }catch(Exception e){
          e.printStackTrace();
      }
        
    }
    
    /*
    决策函数
    input : state : 当前状态
    output : int 返回需要采取的行动, -1表示决策出错
    */
    public int makeDecision(int state){
        List<Integer> maxNumPoses = new ArrayList<>();
        double maxNum=0;
        if(state>=stateNum || state<0){
            return -1;
        }
        int i;
        int action=-1;
        for(i=0;i<actionNum;i++){    //如果有多个Q值相同的话取第一个
           if(i==0){
               maxNum  = QMatrix[i][state];
           }else{
               if(QMatrix[i][state]> maxNum ){
                  maxNum  = QMatrix[i][state];
               }
           }
        }
        for(i=0;i<actionNum;i++){
            if(QMatrix[i][state]==maxNum){
                maxNumPoses.add(i);
            }
        }
        //从相同的值随机选一个
        Random r = new Random();
        System.out.println(maxNumPoses.size());
        int maxPos = 0+r.nextInt(maxNumPoses.size());
        return maxNumPoses.get(maxPos);
    }
    /*
    这个函数 根据 statelast actionlast 和当前的state来更新QMatrix
    input: state 当前的状态
    */
    public void updateQMatrix(int state){
        //根据当前state获取上一步在state下决策的最佳action
        int action=-1;
        //注意这时候的QMatrix是 Q_n-1
        action=makeDecision(state);
        //将Q_n-1 更新成为 Q_n
        if(action!=-1){    //决策有效
            QMatrix[actionLast][stateLast] =(1-alpha)*QMatrix[actionLast][stateLast]+ alpha*( rewardMatrix[actionLast][stateLast]+ r* QMatrix[action][state]); 
            System.out.println("更新训练"+actionLast+" "+stateLast+" "+ QMatrix[actionLast][stateLast] );
        }
    }
    
    //update函数重载，采用动态的方式获取reward,但是reward需要从外部给
    public void updateQMatrix(int state,int reward){
        //根据当前state获取上一步在state下决策的最佳action
        int action=-1;
        //注意这时候的QMatrix是 Q_n-1
        action=makeDecision(state);
        //将Q_n-1 更新成为 Q_n
        if(action!=-1){    //决策有效
            QMatrix[actionLast][stateLast] =(1-alpha)*QMatrix[actionLast][stateLast]+ alpha*(  reward+ r* QMatrix[action][state]); 
            System.out.println("更新训练"+actionLast+" "+stateLast+" "+ QMatrix[actionLast][stateLast] );
        }
    }
    /*
    Qlearning 学习函数
    inputt : state 当前的状态
             isWrite : 是否将更新后的Q矩阵写入到文件中 
    output: 在当前状态下采取的action
    */
    public int learning( int state,boolean isWrite){
    //先根据当前的state更新上一步决策 的Q矩阵
    // 如果不是第一次 ，由于第一次决策没有上一次，所以不进行更新
       int actionNow=-1;  //当前状态下的最佳决策
       if(actionLast!=-1 && stateLast!=-1){
           //先更新
           updateQMatrix(state);
        }else{
           //否则直接决策
           actionNow= makeDecision(state);
           //将当前的状态和决策保存下来，以便于下一次决策前进行Q矩阵的更新
           actionLast=actionNow;
           stateLast=state;
           writeToFile(false);
           return actionNow;
       }
       //更新之后进行决策
       actionNow =  makeDecision(state);
       actionLast=actionNow;
       stateLast=state;
       //根据参数决定是否写入文件
       if(isWrite){
           writeToFile(false);
       }
       //返回action
       return actionNow;
    }
    
   //打印Q矩阵的函数
   public void printQMatrix(){
       if(QMatrix==null){
           System.out.println("Q矩阵还未创建");
       }
       int i,j;
       for(i=0;i<actionNum;i++){
           for(j=0;j<stateNum;j++){
               System.out.print(QMatrix[i][j]+" ");
           }
           System.out.print("\n");
       }
   }
   
    //对learning 函数的重载，采用外部的reward而不是内部的reward矩阵进行学习
    /*
    Qlearning 学习函数
    inputt : state 当前的状态
             reward 外部的r
             isWrite : 是否将更新后的Q矩阵写入到文件中 
    output: 在当前状态下采取的action
    */
    public int learning( int state,int reward,boolean isWrite){
    //先根据当前的state更新上一步决策 的Q矩阵
    // 如果不是第一次 ，由于第一次决策没有上一次，所以不进行更新
       int actionNow=-1;  //当前状态下的最佳决策
       if(actionLast!=-1 && stateLast!=-1){
           //先更新
           updateQMatrix(state,reward);
        }else{
           //否则直接决策
           actionNow= makeDecision(state);
           //将当前的状态和决策保存下来，以便于下一次决策前进行Q矩阵的更新
           actionLast=actionNow;
           stateLast=state;
           writeToFile(false);
           return actionNow;
       }
       //更新之后进行决策
       actionNow =  makeDecision(state);
       actionLast=actionNow;
       stateLast=state;
       //根据参数决定是否写入文件
       if(isWrite){
           writeToFile(false);
       }
       //返回action
       return actionNow;
    }
   
   
   //打印R矩阵的函数
   public void printRewardMatrix(){
       if(rewardMatrix==null){
           System.out.println("R矩阵还未创建");
       }
       int i,j;
       for(i=0;i<actionNum;i++){
           for(j=0;j<stateNum;j++){
               System.out.print(rewardMatrix[i][j]+" ");
           }
           System.out.print("\n");
       }
   }
   public void printLastS_A(){
       System.out.println("lastState"+ stateLast);
         System.out.println("lastAction"+ actionLast);
   }
   

    //测试主函数
   public static void main(String[] agrs){
         Qlearning myQ= new Qlearning(10,50,0.5,0.3,".\\QMatrix.txt",".\\rewardMatrix.txt");
         myQ.printQMatrix();
         myQ.printRewardMatrix();
    }
}
