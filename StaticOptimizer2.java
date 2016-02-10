package optimizers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import objectClasses.Candidates;
import objectClasses.Instance;
import objectClasses.LoadBalancer;
import objectClasses.Plan;
import objectClasses.UserRequest;

public class StaticOptimizer {
	
	public ArrayList<Plan> optimize(UserRequest userRequest,Candidates candidates){
		
		 //存在する全インスタンス数を取得
		 int allInstancesNumber = candidates.getInstances().size();

	     //P(t)を定義する
	     ArrayList<Integer>[] retain = new ArrayList[100];
	     definePopulation(retain,100,allInstancesNumber);
		 
	 	 //Q(t)を定義する
	     ArrayList<Integer>[] population = new ArrayList[100];
		 definePopulation(population,100,allInstancesNumber);
	     
	     //R(t)を定義する
	     ArrayList<Integer>[] merge = new ArrayList[200];
	     definePopulation(merge,200,allInstancesNumber);
	     
	     //解答を格納するArrayList
		 ArrayList<Integer>[] solution = new ArrayList[10];
		 for(int i=0;i<10;i++){
		 solution[i] = new ArrayList<Integer>();
		 }
	     
		 //解答の数を格納するフィールド
	     int solutionNumber=0;
	     
	     //test
	     //Mutation mutation=new Mutation();
	     //mutation.mutationGain(population, allInstancesNumber);
	
	//複数回回す
	for(int loop=0;loop<2;loop++){ 							
		
	     //Q(t)→Q'(t)
	     
	     //交叉
		 crossOverUnits(population);
		 crossOverParts(population);
		 	             
        //突然変異
		 mutationChange(population,allInstancesNumber);
        //mutationReduce(population,allInstancesNumber);
        //mutationGain(population,allInstancesNumber);
	     
	     //R(t)にP(t)とQ'(t)を合成する
	     mergePtQt(population,retain,merge);
	     
	     //evaluateRt配列にの評価を格納
			//初期化
			double evaluateRt[][];
			evaluateRt = new double[200][5];					
			for(int j=0;j<5;j++){
			for(int i=0;i<200;i++){
				evaluateRt[i][j]=0.0;
			}}
	     
		 //高速非優越ソート
		 fastNonDominatedSort(merge,candidates,userRequest,evaluateRt);
		
		 if(loop==0){
			 
			//上位個体を選出 
			solutionNumber = referencePointSelectLast(evaluateRt,merge,solution);
			 
			for(int i=0;i<solutionNumber;i++){
				//System.out.println(solution[i]);
			}
			
		 }else{
			 
		 //参照点による選択と複製
		 referencePointSelect(evaluateRt,population,retain);
		 
		 }

	} 		
	
		//結果をJSON形式に変換・出力
		return output(candidates,userRequest,solution,solutionNumber);			 

}			

/**************************************************************************
*    
*  				以下、利用するメソッド群
* 				
**************************************************************************/

	//populationを定義するメソッド
	private static void definePopulation(ArrayList<Integer>[] population,int Number,int allInstancesNumber) {
		
		for(int i=0;i<Number;i++){
			 population[i] = new ArrayList<Integer>();
			 
			 //各ユニットに1〜10個のインスタンスを確率(5%)で代入する
			 for(int k=0;k<3;k++){	 
			 population[i].add((int)(Math.random()*allInstancesNumber)+1);
			 	for(int j=0;j<9;j++){
			 		if(Math.random()<0.05){
			 			population[i].add((int)(Math.random()*allInstancesNumber)+1);
			 		}
			 	}
			 population[i].add(-1);
			 }
			 //System.out.println(population[i]);
		 }
	
	}
	
	//P(A|B)の計算
	private double relation(int tier,int i, int j, ArrayList<Integer>[] temp,Candidates candidates) {
		
	      int instanceId_1 = new Integer(temp[tier].get(i).toString()).intValue();
	      int instanceId_2 = new Integer(temp[tier].get(j).toString()).intValue();
	      
	      //System.out.println(instanceId_1);
	      //System.out.println(instanceId_2);
	      
	      double relationNumber=0.0;
	      
		//プロバイダが同じか否か
		if(candidates.getInstances().get(instanceId_1-1).getProvider().equals(
				candidates.getInstances().get(instanceId_2-1).getProvider())){
			
			//地域が近さがどうか
			if(candidates.getInstances().get(instanceId_1-1).getLocation().equals(
					candidates.getInstances().get(instanceId_2-1).getLocation())){//同じ
				
				relationNumber = 0.9;
				//System.out.println(relationNumber);
			}else if(LocationShortSameProvider(candidates.getInstances().get(instanceId_1-1).getLocation(),
					candidates.getInstances().get(instanceId_2-1).getLocation(),
					candidates.getInstances().get(instanceId_1-1).getProvider(),
					candidates.getInstances().get(instanceId_2-1).getProvider())){//近い
				
				relationNumber = 0.6;
				//System.out.println(relationNumber);
			}else if(candidates.getInstances().get(instanceId_1-1).getLocation().equals("unknown")||
					 candidates.getInstances().get(instanceId_2-1).getLocation().equals("unknown")){//不明
				
				relationNumber = 0.5;
				//System.out.println(relationNumber);
			}else{//遠い
				
				relationNumber = 0.2;
				//System.out.println(relationNumber);
			}
			
		}else{

			//地域が近さがどうか
			if(candidates.getInstances().get(instanceId_1-1).getLocation().equals(
					candidates.getInstances().get(instanceId_2-1).getLocation())){//同じ
				
				//ありえない
				
			}else if(LocationShortDifferentProvider(candidates.getInstances().get(instanceId_1-1).getLocation(),
					candidates.getInstances().get(instanceId_2-1).getLocation(),
					candidates.getInstances().get(instanceId_1-1).getProvider(),
					candidates.getInstances().get(instanceId_2-1).getProvider())){//近い
				
				relationNumber = 0.5;
				//System.out.println(relationNumber);
			}else if(candidates.getInstances().get(instanceId_1-1).getLocation().equals("unknown")||
					 candidates.getInstances().get(instanceId_2-1).getLocation().equals("unknown")){//不明
				
				relationNumber = 0.1;
				//System.out.println(relationNumber);
			}else{//遠い
				
				relationNumber = 0.5;
				//System.out.println(relationNumber);
			}
			
		}
		
		return relationNumber;
	}
	
	//異なるプロバイダでロケーションが"近い"という判定
	private boolean LocationShortDifferentProvider(String location_1,String location_2,String provider_1,String provider_2) {
		
		boolean flag=false;
		
		if(provider_1.equals("google")){
		
			//アメリカ
			if(location_1.equals("NorthAmerica_US_central1")){
				if(location_2.equals("NorthAmerica_US_Virginia")||location_2.equals("NorthAmerica_US_Oregon")){
					flag = true;
				}
			}
			
			//ヨーロッパ
			if(location_1.equals("Europe_NA_west2")){
				if(location_2.equals("Europe_Ireland_NA")){
					flag = true;
				}
			}
			
			//アジア
			if(location_1.equals("Asia_NA_east1")){
				if(location_2.equals("Asia_Japan_Tokyo")){
					flag = true;
				}
			}
			
		}else if(provider_1.equals("amazon")){
		
			//アメリカ
			if(location_1.equals("NorthAmerica_US_Virginia")||location_1.equals("NorthAmerica_US_Oregon")){
				if(location_2.equals("NorthAmerica_US_central1")){
					flag = true;
				}
			}
			
			//ヨーロッパ
			if(location_1.equals("Europe_Ireland_NA")){
				if(location_2.equals("Europe_NA_west2")){
					flag = true;
				}
			}
			
			//アジア
			if(location_1.equals("Asia_Japan_Tokyo")){
				if(location_2.equals("Asia_NA_east1")){
					flag = true;
				}
			}
			
		}
			
		
		return flag;
	}

	//同一プロバイダで近い地域か
	private boolean LocationShortSameProvider(String location_1,String location_2, String provider_1, String provider_2) {
		
		boolean flag=false;
		
		if(provider_1.equals("google")){
		
			
		}else if(provider_1.equals("amazon")){
		
			//アメリカ
			if(location_1.equals("NorthAmerica_US_Virginia")||location_1.equals("NorthAmerica_US_Oregon")){
				if(location_2.equals("NorthAmerica_US_Virginia")||location_2.equals("NorthAmerica_US_Oregon")){
					flag = true;
				}
			}
			
		}
			
		
		return flag;
	}
	
	//稼働率を返すメソッド
	private double availability(Object tmp,Candidates candidates) {
		
		int instanceId = new Integer(tmp.toString()).intValue();
		double p = 0.0; 
		//System.out.println(candidates.getInstances().get(instanceId).getProvider());
		if(candidates.getInstances().get(instanceId-1).getProvider().equals("google")){
			p = 0.01;
		}else if(candidates.getInstances().get(instanceId-1).getProvider().equals("amazon")){
			p = 0.01;
		}else if(candidates.getInstances().get(instanceId-1).getProvider().equals("rackspace")){
			p = 0.05;
		}else{
			System.out.println("稼働率が不明です");
		}
		//System.out.println(p);
		
		return p;
	}
	
	//Hを計算する
	private int calculateH(int p) {
		
		int H;
		int M=3;
		
		H = (p+M-1)*(p+M-2)/2;
		 //System.out.println(H);
		return H;
	}
	
	//階乗を計算する
	static int factorial(int n){
	        int fact = 1;
	        if (n == 0)
	            return  fact;
	        else { // in case of n > 0
	            for (int i = n; i > 0; i--)
	                fact *= i;
	            return fact;
	        }
	    }
	
	//ランクを出すメソッド
	private void makeRank(int[][] dominate, double[][] evaluate,int number) {
		
		for(int j=0;j<number;j++){
			for(int i=0;i<number-j;i++){
					
			
				if(i==j){
				
					//0が入ったまま
					
				}else{
					
					//全て優越している場合は+1,優越されたら-1.
					if(evaluate[j][0]>evaluate[i+j][0]&&evaluate[j][1]>evaluate[i+j][1]&&evaluate[j][2]>evaluate[i+j][2]){
					
					dominate[j][i+j]=1;
					dominate[i+j][j]=-1;
					
					}else if(evaluate[j][0]<evaluate[i+j][0]&&evaluate[j][1]<evaluate[i+j][1]&&evaluate[j][2]<evaluate[i+j][2]){
				
					dominate[j][i+j]=-1;
					dominate[i+j][j]=1;
				
					}else{
						
						//0が入ったまま
					
					}
					
				}										
		
		}}
		
			
		//dominateした数とされた数の合計を出す
		int dominateNumber[];
		dominateNumber = new int[number];
		for(int j=0;j<number;j++){
			for(int i=0;i<number;i++){
				dominateNumber[j] += dominate[j][i];
				 //System.out.println(dominateNumber[i]);							
			}
		}
										 
		//選択されたら立てるフラグ
		int flag[];
		flag = new int[number];
		for(int i=0;i<number;i++){
			flag[i]=0;
		}
		
		//ランクの決定
		int rank=1;				
		for(int loop=0;loop<10000;loop++){
			 	
			//System.out.println(rank);
			
			//フラグが立っていないものの中で最大のdominate数を調べる
			int maxDominateNumber=-1000;
			for(int i=0;i<number;i++){					
				if(maxDominateNumber<dominateNumber[i]&&flag[i]==0){
					maxDominateNumber=dominateNumber[i];
					//System.out.println(maxDominateNumber);
				}				
			}
			
			//ランクを上から記入
			for(int i=0;i<number;i++){
				if(dominateNumber[i]==maxDominateNumber){
					evaluate[i][3]=rank;
					flag[i]=1;
					 //System.out.print(evaluate[i][3]);
						
				}
			}
			
			//終了条件はflagの和がnumberになること
			int sumflag=0;
			for(int i=0;i<number;i++){
				sumflag += flag[i];
			}
			
			if(sumflag==number){
				break;
			}
			
			rank++;
			
			if(loop==9999){
				System.out.println("失敗");
			}
			
		}
						
		 //デバッグ用print
		 for(int i=0;i<100;i++){
		 //System.out.println(evaluate[i][3]);
		 }
		 			
	}
	
	//参照点の作成
	private void makeReferencePoint(double[][] referencePoint,int p) {
	
		int total = 0;
		
	for(int j=0;j<p+1;j++){				
		
		for(int i=0;i<j+1;i++){
			double z = 1.0 - (1.0/(double)p)*j;
			double c = 1.0-z*z;
			//System.out.println(Math.sqrt(c));
			if(i==0){
				referencePoint[total][0]=Math.sqrt(c);
				referencePoint[total][1]=0;
				referencePoint[total][2]=z;
				//System.out.println(total+" , "+"(a,b,c)=("+referencePoint[total][0]+" , "+referencePoint[total][1]+" , "+referencePoint[total][2]+")");
				total++;
				//System.out.println("(a,b)=("+0+","+j+")");
			}else{
			    double a = i;
				double b = j-i;

				referencePoint[total][0]=(b/a)*Math.sqrt(c/(1+((b*b)/(a*a))));
				referencePoint[total][1]=Math.sqrt(c/(1+((b*b)/(a*a))));
				referencePoint[total][2]=z;						
				
				//System.out.println(total+" , "+"(x,y,z)=("+referencePoint[total][0]+" , "+referencePoint[total][1]+" , "+referencePoint[total][2]+")");
				//System.out.println("(a,b)=("+a+","+b+")");
				total++;
				
			}
			
		}
		//System.out.println();
		
	}
		
		
	}  
		
	//距離を求めるメソッド
	private double distance(double x,double y,double z){				
		return Math.sqrt(Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2));
	}
	
	//目的関数price
	private void price(ArrayList<Integer>[] population, Candidates candidates,double[][] evaluate,int number) {
		
		 
		for(int loop=0;loop<number;loop++){
		
		//一時保存用ArrayList
		ArrayList temp = new ArrayList();
		
		//InsTypeだけ取り出す
		for(int i=0;i<population[loop].size();i++){
			
			//object型→int型に変換
			//int decide = new Integer(population[loop].get(i).toString()).intValue();
			int decide = population[loop].get(i);
			//-1は仕切りなので要素に入れない＆仕切りでtempの入れ替わり
			if(decide==-1){
				//何もしない
			}else{
				temp.add(population[loop].get(i));
				
			}
				
		}
		//System.out.println(temp);
		
		
		Iterator iter = temp.iterator();  // 各要素に触るための Iterator を取得
	    while(iter.hasNext()) {            // 要素がある間は、
	      Object tmp = iter.next();        // Object next() で取得
	      int instanceId = new Integer(tmp.toString()).intValue();
	      //System.out.println("id : "+instanceId+" price : "+candidates.getInstances().get(instanceId));
	      //System.out.println(tmp+"のインスタンス金額："+candidates.getInstances().get(instanceId	-1).getPrice());
	      evaluate[loop][0]+=candidates.getInstances().get(instanceId-1).getPrice();	

	    }
		
	    //System.out.println("合計金額:"+evaluate[loop][0]);
	  
	    //正規化
	    evaluate[loop][0] = Math.exp(-evaluate[loop][0]);
		
		//System.out.println("正規化金額:"+evaluate[loop][0]);
		
	}
		
		
	}

	//目的関数performance
	private void performance(UserRequest userRequest,ArrayList<Integer>[] population, Candidates candidates,double[][] evaluate,int number) {
		
		for(int loop=0;loop<number;loop++){
			evaluate[loop][1] = Math.random();
		}
		
		
		/*
		for(int loop=0;loop<number;loop++){
			evaluate[loop][1] = distance(1,1,1);
			
		//正規化
	    evaluate[loop][1] = Math.exp(-evaluate[loop][1]);
		}
		*/
		
	}

	//目的関数falutTorelance
	private void faultTorelance(ArrayList<Integer>[] population,Candidates candidates, double[][] evaluate,int number) {
	
		for(int loop=0;loop<number;loop++){
						
		//populationの分解
		//一時保存用ArrayList
		ArrayList[] temp=new ArrayList[3];
		for(int i=0;i<3;i++){
		temp[i]=new ArrayList();
		}
		
		//比較用配列
		double p[];
		p = new double[3];
		
		//ユニットごとに代入
			int count = 0;
		for(int i=0;i<population[loop].size();i++){
			
			//object型→int型に変換
			int decide = new Integer(population[loop].get(i).toString()).intValue();
			
			//-1は仕切りなので要素に入れない＆仕切りでtempの入れ替わり
			if(decide==-1){
				count++;
			}else if(count==0){
				temp[0].add(population[loop].get(i));
			}else if(count==1){
				temp[1].add(population[loop].get(i));
			}else if(count==2){
				temp[2].add(population[loop].get(i));
			}
		}
		
		//三層をそれぞれ計算する
		for(int tier=0;tier<3;tier++){
			
			int size = temp[tier].size();
			//System.out.println(size);
			
			for(int i=0;i<size;i++){//sizeの数だけ項数
				
				double member=1.0;
				
				for(int j=0;j<size;j++){//sizeの数だけ要素数
				
					if(j==size-1){
						
						member *= availability(temp[tier].get(i),candidates);//P(A)はプロバイダごと
						
					}else{
						
						member *= relation(tier,(j+i+1)%size,i%size,temp,candidates);//P(A|B)
						
					}
					
				}
			
			p[tier] += member;
				
			}
			
		}

		
		//System.out.println(p[0]);
		//System.out.println(p[1]);
		//System.out.println(p[2]);
		
		//三層でもっとも弱いところを評価値とする
		if(p[0]>=p[1]&&p[0]>=p[2]){
			evaluate[loop][2]=p[0];
			//System.out.println(evaluate[loop][2]);
		}else if(p[1]>=p[0]&&p[1]>=p[2]){
			evaluate[loop][2]=p[1];
			//System.out.println(evaluate[loop][2]);
		}else if(p[2]>=p[0]&&p[2]>=p[1]){
			evaluate[loop][2]=p[2];
			//System.out.println(evaluate[loop][2]);
		}else{
			System.out.println("失敗");
		}

		//正規化
		evaluate[loop][2]=1+1/(Math.log(evaluate[loop][2]));
		//System.out.println(evaluate[loop][2]);
		}
		
		
	}

	//制約充足問題ソルバー
	private void satisfy(ArrayList<Integer>[] population,Candidates candidates, double[][] evaluate,int number) {
						
		for(int i=0;i<number;i++){
		
			double	distance = distance(evaluate[i][0],evaluate[i][1],evaluate[i][2]);
		
			//正規化後で距離1.5以上
			if(distance>1.5){
				evaluate[i][3]=1;
			}											
			//System.out.println(evaluate[i][3]);
			
		}
		
		
	}
				
	//高速非優越ソート
	private void fastNonDominatedSort(ArrayList<Integer>[] population,Candidates candidates,UserRequest userRequest,double[][] evaluateRt) {
		
		//個体数
		int populationNumber = 200;
		
		 //priceの計算
        price(population,candidates,evaluateRt,populationNumber);
		
		 //performanceの計算	
		 performance(userRequest,population,candidates,evaluateRt,populationNumber);					
		
		 //fault toleranceの計算
		 faultTorelance(population,candidates,evaluateRt,populationNumber);
		
		 //制約充足のチェック
		 //satisfy(population,candidates,evaluateRt,populationNumber);				 
		
		 //dominate配列に優越・被優越を格納
			//初期化
			int dominate[][];
			dominate = new int[populationNumber][populationNumber];					
			for(int j=0;j<populationNumber;j++){
			for(int i=0;i<populationNumber;i++){
				dominate[i][j]=0;
			}}
			
		 //ランクの作成
		 makeRank(dominate,evaluateRt,populationNumber);
				 
	}

	//参照点による選択(200→100)
	private void referencePointSelect(double[][] evaluate,ArrayList<Integer>[] population,ArrayList<Integer>[] retain) {
	
	  int H;
	  int p=19;
	  H = calculateH(p);
	  //参照点の座標(x,y,z)とニッチカウントρ
	  double referencePoint [][] = new double[H][4];
	  ArrayList selectedPopulation = new ArrayList();
	  
	  //参照点の作成
	  makeReferencePoint(referencePoint,p);

	  //参照点による選択
	  select(referencePoint,evaluate,H,selectedPopulation,population,retain);
		
	  //選択したPopulationを複製
	  //duplicate(selectedPopulation,population,retain);
	  
	}

	//200→少数→100に複製
	private void duplicate(ArrayList<Integer> selectedPopulation,ArrayList<Integer>[] population,ArrayList<Integer>[] retain) {
		
		int size = selectedPopulation.size();
		
		ArrayList tempPopulation[];
		tempPopulation = new ArrayList[100];
		for(int i=0;i<100;i++){
			tempPopulation[i] = new ArrayList();
			}
		
		for(int loop=0;loop<100;loop++){
			int random = (int)(Math.random()*size);
			//System.out.println(size);
			//System.out.println(random);
			int duplicateNumber = new Integer(selectedPopulation.get(random).toString()).intValue();
			//System.out.println(duplicateNumber);
			if(duplicateNumber<100){
				tempPopulation[loop].addAll(retain[duplicateNumber]);
			}else if(100<=duplicateNumber && duplicateNumber<200){
				tempPopulation[loop].addAll(population[duplicateNumber-100]);	
			}else{
				System.out.println("桁溢れ");
			}
		}
		
		//t→t+1のために削除
		for(int i=0;i<100;i++){
		population[i].clear();
		retain[i].clear();
		}
		
		//複製
		for(int i=0;i<100;i++){
			population[i].addAll(tempPopulation[i]);
			retain[i].addAll(tempPopulation[i]);
			
		}
			
		
	}

	//選択部分
	private void select(double[][] referencePoint, double[][] evaluate,int H,ArrayList<Integer> selectedPopulation,ArrayList<Integer>[] population,ArrayList<Integer>[] retain) {
		
	//rankごとのPopulation番号を入れるArrayList
	ArrayList<Integer>[] rankPopulation = new ArrayList[200];
	for(int i=0;i<200;i++){
		rankPopulation[i]= new ArrayList<Integer>();
	}
	
	//rankごとにPopulation番号を格納
	for(int j=0;j<200;j++){
	for(int i=0;i<200;i++){
		
		//制約充足をかけるときはswitchをこれで囲む
		//if(evaluate[i][4]==0){}
		
		//rankの大きさが1〜ならばArrayListに格納
		if(evaluate[i][3]==j){
			//System.out.println( "evaluate[i][3]= "+evaluate[i][3]);
			rankPopulation[j].add(i);
			//System.out.println("rankPopulation = "+rankPopulation[j]);
			
		}
	}
	}
	
	/*
	System.out.println();
	for(int i=0;i<200;i++){
		System.out.println(rankPopulation[i]);
	}
	*/
	
	int i=0;
	do{
		
		Iterator iter = rankPopulation[i].iterator();  
	    while(iter.hasNext()) {            
	      
	      //Population番号を取得
	      Object tmp = iter.next();        
	      int populationNumber = new Integer(tmp.toString()).intValue();
	      
	      //System.out.println(populationNumber);
	  		      
		selectedPopulation.add(populationNumber);
		
	    }
	    
	    
		i++;	
		
	}while(selectedPopulation.size()+rankPopulation[i].size()<101);
	
	//System.out.println("selected:"+selectedPopulation.size());
	//System.out.println("rank next:"+rankPopulation[i].size());
	
	//System.out.println("selectedPopulation = "+selectedPopulation);
	//System.out.println("selectedPopulationSize = "+selectedPopulation.size());
	
	//パレートフロントlまでがちょうど100なら参照点を使わない
	//100でないとき参照点によって選択する
	if(selectedPopulation.size()==100){
		
		int size = selectedPopulation.size();
		
		ArrayList tempPopulation[];
		tempPopulation = new ArrayList[100];
		for(i=0;i<100;i++){
			tempPopulation[i] = new ArrayList();
			}
		
		
		i=0;
		//populationとretainの一時保管
		Iterator iter = selectedPopulation.iterator();  
	    while(iter.hasNext()) {            
	      //Population番号を取得
	      Object tmp = iter.next();        
	      int populationNumber = new Integer(tmp.toString()).intValue();
	      //System.out.println(populationNumber);
	      if(populationNumber<100){
	      tempPopulation[i].addAll(population[populationNumber]);
	      }else{
	      tempPopulation[i].addAll(retain[populationNumber-100]);      
	      }
	    i++;
		//System.out.println("tempPopulation = "+tempPopulation[i-1]);
	    }
		
		//t→t+1のために削除
		for(i=0;i<100;i++){
		population[i].clear();
		retain[i].clear();
		}
		
		//Pt+1 = St, Qt+1 = St
		for(i=0;i<100;i++){
			population[i].addAll(tempPopulation[i]);
			retain[i].addAll(tempPopulation[i]);
		}
		
	}else{
		
		//参照点によって選ばれる数
		int referencePointSelectNumber = 100- selectedPopulation.size();
		
		
		for(i=0;i<200;i++){
			
			int minNumber = 0;
			double minDistance = 0;
			
			for(int j=0;j<H;j++){
				
				double distance = distance(
			  			  (referencePoint[j][0]-evaluate[i][0]),
			  			  (referencePoint[j][1]-evaluate[i][1]),
			  			  (referencePoint[j][2]-evaluate[i][2]));;
				
				if(j==0){
					minNumber = 0;
					minDistance = distance;
					//System.out.println("1-st minDistance: "+minDistance);	
				}
				
				if(minDistance > distance){
					minDistance = distance;
					minNumber = j; 
					//System.out.println("minNumber: "+minNumber);
					//System.out.println("minDistance: "+minDistance);	
					
				}		
				
			}
			
			//niche-count no zouka
			referencePoint[minNumber][3]++;
			
		}
		
		/*
		for(i=0;i<H;i++){
		System.out.println("ro- :"+referencePoint[i][3]);
		}
		*/
		
		/*
		 * seiyaku jyoken niyoru tournament operater
		 * mi jissou
		 */
		
		//mottomo ro- ga hikui referencePoint ni kanren zuita kotai wo sentaku
		ArrayList sortNicheCount;
		sortNicheCount = new ArrayList();
		for(i=0;i<H;i++){
			sortNicheCount = new ArrayList();
			}
		
		for(i=0;i<H;i++){
			sortNicheCount.add(referencePoint[i][3]);
		}
		
		//System.out.println(sortNicheCount);
		Collections.sort(sortNicheCount);
	
		System.out.println(sortNicheCount);
		
		
	}
		/*
		//Population番号を取得
		Iterator iter = rankPopulation[rank-1].iterator(); 
	      Object tmp = iter.next();        
	      int populationNumber = new Integer(tmp.toString()).intValue();
	    
	    		
	      //最小距離の保存用(0番目の距離)
	      double minDistance=distance(
  			  (referencePoint[0][0]-evaluate[0][0]),
  			  (referencePoint[0][1]-evaluate[0][1]),
  			  (referencePoint[0][2]-evaluate[0][2]));;
	    
	      //最小距離のときの参照点のナンバー
	      int minNumber=0;
	      
	      //距離を出す
	      for(int j=0;j<H;j++){
	    	  
	    	  //参照点とあるPopulationとの距離(x,y,z)
	    	  double distance =distance(
	    			  (referencePoint[j][0]-evaluate[populationNumber][0]),
	    			  (referencePoint[j][1]-evaluate[populationNumber][1]),
	    			  (referencePoint[j][2]-evaluate[populationNumber][2]));
	    	  
	    	  
	    	  //今までで距離最小なら格納
	    	  if(minDistance>distance){			    		  
	    		  minDistance = distance;
	    		  minNumber = j;		
	    	  }			    	  
	      }
	      */
	      
	      /*
	       //100になるまで追加
	       for(i=0;i<referencePointSelectNumber;i++){
	       追加する個体番号[i];
	       selectedPopulation.add(追加する個体番号);
	       }
	       */
		
		//System.out.println("selectedPopulation");
	
	
	
	
	
	/*
		
		
		//ランクごとに処理
		for(int rank=1;rank<201;rank++){
		Iterator iter = rankPopulation[rank-1].iterator();  
	    while(iter.hasNext()) {            
	      
	      //Population番号を取得
	      Object tmp = iter.next();        
	      int populationNumber = new Integer(tmp.toString()).intValue();
	      
	      //最小距離の保存用(hajime ha 0banme no distance)
	      double minDistance=distance(
    			  (referencePoint[0][0]-evaluate[populationNumber][0]),
    			  (referencePoint[0][1]-evaluate[populationNumber][1]),
    			  (referencePoint[0][2]-evaluate[populationNumber][2]));;
	    
	      //最小距離のときの参照点のナンバー
	      int minNumber=0;
	      
	      //距離を出す
	      for(int j=1;j<H;j++){
	    	  
	    	  //参照点とあるPopulationとの距離(x,y,z)
	    	  double distance =distance(
	    			  (referencePoint[j][0]-evaluate[populationNumber][0]),
	    			  (referencePoint[j][1]-evaluate[populationNumber][1]),
	    			  (referencePoint[j][2]-evaluate[populationNumber][2]));
	    	  
	    	  
	    	  //今までで距離最小なら格納
	    	  if(minDistance>distance){			    		  
	    		  minDistance = distance;
	    		  minNumber = j;		
	    	  }			    	  
	      }
	      
	      //選択
	      if(selected[minNumber]==true){
	    	  selectedPopulation.add(populationNumber);
	    	  selected[minNumber]=false;
	      }
	      
	    }
		}
	    */
	
	
		
		
		
	}

	//交叉（Webサーバ・DBサーバごと）	
	private static void crossOverUnits(ArrayList<Integer>[] population) {
		
		for(int loop=0;loop<100;loop=loop+2){
		
		//printOne(population,0+loop);
		//printOne(population,1+loop);
		
		//一時保存用ArrayList
		//0,1はユニット１用、2,3はユニット２用、4,5はユニット３用
		ArrayList<Integer>[] temp=new ArrayList[6];
		for(int i=0;i<6;i++){
		temp[i]=new ArrayList<Integer>();
		}
		
		//ユニットごとに代入
		for(int j=0;j<2;j++){
			int count = 0;
		for(int i=0;i<population[j+loop].size();i++){
			
			//object型→int型に変換
			int decide = new Integer(population[j+loop].get(i).toString()).intValue();
			
			//-1は仕切りなので要素に入れない＆仕切りでtempの入れ替わり
			if(decide==-1){
				count++;
			}else if(count==0){
				temp[j].add(population[j+loop].get(i));
			}else if(count==1){
				temp[j+2].add(population[j+loop].get(i));
			}else if(count==2){
				temp[j+4].add(population[j+loop].get(i));
			}
		}
		count=0;
		}
		
		//削除
		for(int i=0+loop;i<2+loop;i++){
		population[i].clear();
		}
		
		//挿入
		//population[0]のWebサーバ←population[1]のWebサーバ
		Iterator  ite0 = temp[0].iterator(); 
		while(ite0.hasNext()) {              
			population[1+loop].add((int)ite0.next());        
		}
		population[1+loop].add(-1);
		
		//population[1]のWebサーバ←population[0]のWebサーバ
		Iterator  ite1 = temp[1].iterator(); 
		while(ite1.hasNext()) {           
			population[0+loop].add((int)ite1.next());        
		}
		population[0+loop].add(-1);

		//真ん中は交叉しない場合
		//population[0]のAppサーバ←population[0]のAppサーバ
		Iterator  ite2 = temp[2].iterator(); 
		while(ite2.hasNext()) {           
			population[0+loop].add((int)ite2.next());        
		}
		population[0+loop].add(-1);
				
		//population[1]のAppサーバ←population[1]のAppサーバ
		Iterator  ite3 = temp[3].iterator(); 
		while(ite3.hasNext()) {           
			population[1+loop].add((int)ite3.next());        
		}
		population[1+loop].add(-1);
		
		
/*		//交叉する場合
		//population[0]のAppサーバ←population[1]のAppサーバ
		Iterator  ite2 = temp[2].iterator(); 
		while(ite2.hasNext()) {           
			population[1+loop].add(ite2.next());        
		}
		population[1+loop].add(-1);
		
		//population[1]のAppサーバ←population[0]のAppサーバ
		Iterator  ite3 = temp[3].iterator(); 
		while(ite3.hasNext()) {           
			population[0+loop].add(ite3.next());        
		}
		population[0+loop].add(-1);
*/
		
		//population[0]のDBサーバ←population[1]のDBサーバ
		Iterator  ite4 = temp[4].iterator(); 
		while(ite4.hasNext()) {           
			population[1+loop].add((int)ite4.next());        
		}
		population[1+loop].add(-1);
		
		//population[1]のDBサーバ←population[0]のDBサーバ
		Iterator  ite5 = temp[5].iterator(); 
		while(ite5.hasNext()) {           
			population[0+loop].add((int)ite5.next());        
		}
		population[0+loop].add(-1);
		
		//printOne(population,0+loop);
		//printOne(population,1+loop);
				
		}
		
	}

	//交叉（一つ一つのインスタンスを一様交叉）
	private static void crossOverParts(ArrayList<Integer>[] population) {
		
		for(int loop=0;loop<100;loop=loop+2){
			
				//一時保存用ArrayList
				//0,1はユニット１用、2,3はユニット２用、4,5はユニット３用
				ArrayList<Integer>[] temp=new ArrayList[6];
				for(int i=0;i<6;i++){
				temp[i]=new ArrayList<Integer>();
				}
				
				//ユニットごとに代入
				for(int j=0;j<2;j++){
					int count = 0;
				for(int i=0;i<population[j+loop].size();i++){
					
					//object型→int型に変換
					int decide = new Integer(population[j+loop].get(i).toString()).intValue();
					
					//-1は仕切りなので要素に入れない＆仕切りでtempの入れ替わり
					if(decide==-1){
						count++;
					}else if(count==0){
						temp[j].add(population[j+loop].get(i));
					}else if(count==1){
						temp[j+2].add(population[j+loop].get(i));
					}else if(count==2){
						temp[j+4].add(population[j+loop].get(i));
					}
				}
				count=0;
				}
				
				//削除
				population[loop].clear();
				population[loop+1].clear();
				
				//一様交叉
				for(int j=0;j<6;j=j+2){
					
				if(temp[j].size()>temp[j+1].size()){
					
					int differance = temp[j].size() - temp[j+1].size();
					int same = temp[j].size()-differance;
					
					//同一部分の回数はそれぞれの入れ替え
					for(int i=0;i<same;i++){
						//それぞれの交叉確率=0.1
						if(Math.random()<0.1){
							population[loop].add(temp[j+1].get(i));
							population[loop+1].add(temp[j].get(i));
						}else{
							population[loop].add(temp[j].get(i));
							population[loop+1].add(temp[j+1].get(i));	
						}
					}
					
					//差分は片方を片方に移動するかどうか
					for(int i=0;i<differance;i++){
						//それぞれの交叉確率=0.1
						if(Math.random()<0.1){
							population[loop+1].add(temp[j].get(i+same));
						}else{
							population[loop].add(temp[j].get(i+same));
						}
					}
					
					population[loop].add(-1);
					population[loop+1].add(-1);	
					
				}else if(temp[j].size()==temp[j+1].size()){
					
					for(int i=0;i<temp[j].size();i++){
						//それぞれの交叉確率=0.1
						if(Math.random()<0.1){
							population[loop].add(temp[j+1].get(i));
							population[loop+1].add(temp[j].get(i));
						}else{
							population[loop].add(temp[j].get(i));
							population[loop+1].add(temp[j+1].get(i));	
						}
						
					}
					
					population[loop].add(-1);
					population[loop+1].add(-1);	
					
				}else{
					
					int differance = temp[j+1].size() - temp[j].size();
					int same = temp[j+1].size()-differance;
					
					//同一部分の回数はそれぞれの入れ替え
					for(int i=0;i<same;i++){
						//それぞれの交叉確率=0.1
						if(Math.random()<0.1){
							population[loop].add(temp[j+1].get(i));
							population[loop+1].add(temp[j].get(i));
						}else{
							population[loop].add(temp[j].get(i));
							population[loop+1].add(temp[j+1].get(i));	
						}
					}
					
					//差分は片方を片方に移動するかどうか
					for(int i=0;i<differance;i++){
						//それぞれの交叉確率=0.1
						if(Math.random()<0.1){
							population[loop].add(temp[j+1].get(i+same));
						}else{
							population[loop+1].add(temp[j+1].get(i+same));
						}
					}
					
					population[loop].add(-1);
					population[loop+1].add(-1);	
					
				}
				}
		}
		
	}
	
	//突然変異：変更
	private void mutationChange(ArrayList<Integer>[] population, int allInstancesNumber) {

		for(int loop=0;loop<100;loop++){
			
			//突然変異確率
			if(Math.random()<0.1){
			
			//一時保存用ArrayList
			ArrayList<Integer>[] temp=new ArrayList[3];
			for(int i=0;i<3;i++){
			temp[i]=new ArrayList<Integer>();
			}
			
			//ユニットごとに代入
				int count = 0;
			for(int i=0;i<population[loop].size();i++){
				
				//object型→int型に変換
				int decide = new Integer(population[loop].get(i).toString()).intValue();
				
				//-1は仕切りなので要素に入れない＆仕切りでtempの入れ替わり
				if(decide==-1){
					count++;
				}else if(count==0){
					temp[0].add((int)population[loop].get(i));
				}else if(count==1){
					temp[1].add((int)population[loop].get(i));
				}else if(count==2){
					temp[2].add((int)population[loop].get(i));
				}
			}
			
			//populationの中身を削除
			population[loop].clear();
		

					//要素が変わる
					//要素を変えるユニットを確定
					int changeUnit=0;
					if(Math.random()<(1/3)){
						changeUnit=0;
					}else if((1/3)<=Math.random() && Math.random()<(2/3)){
						changeUnit=1;
					}else{
						changeUnit=2;
					}
					
					//要素を変える場所を確定←要素の数ぶんの１
					int changeParts = (int) ((Math.random()*100+1)%temp[changeUnit].size());
					
					temp[changeUnit].set(changeParts, (int)(Math.random()*allInstancesNumber)+1);
											
				
				//要素の挿入
				for(int i=0;i<3;i++){
				Iterator  ite = temp[i].iterator(); 
				while(ite.hasNext()) {              
					population[loop].add((int)ite.next());        
				}
				population[loop].add(-1);
				}												
				
			}
			
		}
		
	}
	
	//突然変異：減少
	private void mutationReduce(ArrayList<Integer>[] population, int allInstancesNumber) {
		for(int loop=0;loop<100;loop++){
			
			//突然変異確率
			if(Math.random()<0.1){
			
			//一時保存用ArrayList
			ArrayList<Integer>[] temp=new ArrayList[3];
			for(int i=0;i<3;i++){
			temp[i]=new ArrayList<Integer>();
			}
			
			//ユニットごとに代入
				int count = 0;
			for(int i=0;i<population[loop].size();i++){
				
				//System.out.println(population[loop].get(i));
				//object型→int型に変換
				int decide = population[loop].get(i);
				
				//-1は仕切りなので要素に入れない＆仕切りでtempの入れ替わり
				if(decide==-1){
					count++;
				}else if(count==0){
					temp[0].add(population[loop].get(i));
				}else if(count==1){
					temp[1].add(population[loop].get(i));
				}else if(count==2){
					temp[2].add(population[loop].get(i));
				}
			}
			
			//populationの中身を削除
			population[loop].clear();
					
					//要素が変わる
					//要素を変えるユニットを確定
					int changeUnit=0;
					if(Math.random()<(1/3)){
						changeUnit=0;
					}else if((1/3)<=Math.random() && Math.random()<(2/3)){
						changeUnit=1;
					}else{
						changeUnit=2;
					}
					
					//要素を変える場所を確定←要素の数ぶんの１
					int changeParts = ((int) ((Math.random()*100)+1))%temp[changeUnit].size();
					
					temp[changeUnit].set(changeParts, (int)(Math.random()*allInstancesNumber)+1);
					
										
				//要素の挿入
				for(int i=0;i<3;i++){
				Iterator  ite = temp[i].iterator(); 
				while(ite.hasNext()) {              
					population[loop].add((int)ite.next());        
				}
				population[loop].add(-1);
				}												
				
			}
			
		}
		
	}
	
	//突然変異：増加
	private void mutationGain(ArrayList<Integer>[] population, int allInstancesNumber) {
		for(int loop=0;loop<100;loop++){
			
			//突然変異確率
			if(Math.random()<0.1){
			
			//一時保存用ArrayList
			ArrayList<Integer>[] temp=new ArrayList[3];
			for(int i=0;i<3;i++){
			temp[i]=new ArrayList<Integer>();
			}
			
			//ユニットごとに代入
				int count = 0;
			for(int i=0;i<population[loop].size();i++){
				
				//System.out.println(population[loop].get(i));
				//object型→int型に変換
				int decide = population[loop].get(i);
				//System.out.println(decide);
				
				//-1は仕切りなので要素に入れない＆仕切りでtempの入れ替わり
				if(decide==-1){
					count++;
				}else if(count==0){
					temp[0].add(population[loop].get(i));
				}else if(count==1){
					temp[1].add(population[loop].get(i));
				}else if(count==2){
					temp[2].add(population[loop].get(i));
				}
			}
			
			//populationの中身を削除
			population[loop].clear();
		
					//要素が増える
					//要素が増えるユニットを確定
					int changeUnit=0;
					if(Math.random()<(1/3)){
						changeUnit=0;
					}else if((1/3)<=Math.random() && Math.random()<(2/3)){
						changeUnit=1;
					}else{
						changeUnit=2;
					}
					
					//要素を増やす場所を確定←要素の数ぶんの１
					int changeParts = (int) ((Math.random()*100+1)%temp[changeUnit].size());
					
					temp[changeUnit].add(changeParts, (int)(Math.random()*allInstancesNumber)+1);
					
								
				//要素の挿入
				for(int i=0;i<3;i++){
				Iterator  ite = temp[i].iterator(); 
				while(ite.hasNext()) {              
					population[loop].add((int)ite.next());        
				}
				population[loop].add(-1);
				}												
				
			}
			
		}
		
	}
			
	//R(t)=P(t)+Q(t)の作成
	private void mergePtQt(ArrayList<Integer>[] population, ArrayList<Integer>[] retain,ArrayList<Integer>[] merge) {
		
		for(int i=0;i<100;i++){
			merge[i].addAll(retain[i]);//P(t)
		}								
		
		for(int i=0;i<100;i++){
			merge[i+100].addAll(population[i]);//Q(t)
		}	
		
		

	}
	
	//解の作成
	private int referencePointSelectLast(double[][] evaluateRt,ArrayList<Integer>[] merge, ArrayList<Integer>[] solution) {

		int H;
		  int p=19;
		  H = calculateH(p);
		  double referencePoint [][] = new double[H][3];
		  ArrayList<Integer> selectedPopulation = new ArrayList<Integer>();
		  
		  //参照点の作成
		  makeReferencePoint(referencePoint,p);

		  //参照点による選択
		  int solutionNumber = selectLast(referencePoint,evaluateRt,H,selectedPopulation,solution,merge);
		
		  return solutionNumber;
	}
	
	//最終的な選択
	private int selectLast(double[][] referencePoint,double[][] evaluate, int H, ArrayList<Integer> selectedPopulation,ArrayList<Integer>[] solution,ArrayList<Integer>[] merge) {
		
		//rankごとのPopulation番号を入れるArrayList
		ArrayList<Integer>[] rankPopulation = new ArrayList[10];
		for(int i=0;i<10;i++){
			rankPopulation[i]= new ArrayList<Integer>();
		}
		
		//rankごとにPopulation番号を格納
		for(int i=0;i<200;i++){
			
			//制約充足をかけるときはswitchをこれで囲む
			//if(evaluate[i][4]==0){}
			
			//rankの大きさが1〜10ならばArrayListに格納
			switch((int)evaluate[i][3]){
			case 1:
				rankPopulation[0].add(i);
				break;
				
			case 2:
				rankPopulation[1].add(i);
				break;
			
			case 3:
				rankPopulation[2].add(i);
				break;
				
			case 4:
				rankPopulation[3].add(i);
				break;
				
			case 5:
				rankPopulation[4].add(i);
				break;
				
			case 6:
				rankPopulation[5].add(i);
				break;
				
			case 7:
				rankPopulation[6].add(i);
				break;
				
			case 8:
				rankPopulation[7].add(i);
				break;
				
			case 9:
				rankPopulation[8].add(i);
				break;
				
			case 10:
				rankPopulation[9].add(i);
				break;
			
			}
		}
			
			//選択された参照点を示す
			boolean selected[];
			selected = new boolean[H];
			for(int j=0;j<H;j++){
				selected[j]=true;
			}
			
			//ランクごとに処理
			for(int rank=1;rank<11;rank++){
			Iterator iter = rankPopulation[rank-1].iterator();  
		    while(iter.hasNext()) {            
		      
		      //Population番号を取得
		      Object tmp = iter.next();        
		      int populationNumber = new Integer(tmp.toString()).intValue();
		      
		      //最小距離の保存用
		      double minDistance=1000;
		    
		      //最小距離のときの参照点のナンバー
		      int minNumber=0;
		      
		      //距離を出す
		      for(int j=0;j<H;j++){
		    	  
		    	  //参照点とあるPopulationとの距離(x,y,z)
		    	  double distance =distance(
		    			  (referencePoint[j][0]-evaluate[populationNumber][0]),
		    			  (referencePoint[j][1]-evaluate[populationNumber][1]),
		    			  (referencePoint[j][2]-evaluate[populationNumber][2]));
		    	  
		    	  //今までで距離最小なら格納
		    	  if(minDistance>distance){			    		  
		    		  minDistance = distance;
		    		  minNumber = j;		
		    	  }			    	  
		      }
		      
		      //選択
		      if(selected[minNumber]==true){
		    	  selectedPopulation.add(populationNumber);
		    	  selected[minNumber]=false;
		      }
		      
		    }
			}
		    
			for(int i=0;i<selectedPopulation.size();i++){
				
				int populationId = new Integer(selectedPopulation.get(i).toString()).intValue();
				
				solution[i].addAll(merge[populationId]);
				
				if(i==9){
					break;
				}
			}
		
		
			//System.out.println(solution);
		
			return selectedPopulation.size();
			
	}

	//出力
	public ArrayList<Plan> output(Candidates candidates,UserRequest userRequest,ArrayList<Integer>[] solution,int solutionNumber) {
		
		ArrayList<Plan> planList=new ArrayList<Plan>();
		
		for(int i=0;i<solutionNumber;i++){
			ArrayList<Instance> instances=new ArrayList<Instance>();
			ArrayList loadbalancer=new ArrayList();
			for(int j=0;j<solution[i].size();j++){
				int instanceNum = new Integer(solution[i].get(j).toString()).intValue();
				//-1は無視
				if(instanceNum==-1){
					continue;
				}else{ 
					//Instance型のオブジェクトを取得
					Instance instance=candidates.getInstances().get(instanceNum-1);
					//System.out.println(instance);
					//listに追加
					instances.add(instance);
				}
			}
			Plan plan = new Plan(instances,loadbalancer);
			//System.out.println("plan add");
			planList.add(plan);
		}
		
		
		/*
		//プラン５（amazonのVM二つ、LB一つ）
		ArrayList<Instance> list1_1=new ArrayList<Instance>();  // インスタンスのリスト
		ArrayList<LoadBalancer> list1_2=new ArrayList<LoadBalancer>();  //　LBのリスト	
		// インスタンス5_1
		Instance instance1_1 = new Instance(1, "amazonVM1", "t1.micro", 1, 2, 100, "SSD","CentOS6","NorthAmerica_US_Virginia", "amazon",0.33);
		Instance instance1_2 = new Instance(2, "amazonVM2", "t1.micro", 1, 2, 100, "SSD","CentOS6","NorthAmerica_US_Virginia", "amazon",0.33);
		list1_1.add(instance1_1);
		list1_1.add(instance1_2);
		LoadBalancer lb1_1=new LoadBalancer(1,"amazonLB1","ElasticLoadBalancing","amazon","NorthAmerica_US_Virginia","1,2");
		list1_2.add(lb1_1);
		Plan plan1=new Plan(list1_1,list1_2);
		planList.add(plan1);
		*/
		
		
		/*
		// プラン1(amazonのVM一つ)
		ArrayList<Instance> list1_1=new ArrayList<Instance>();  // インスタンスのリスト
		ArrayList<LoadBalancer> list1_2=new ArrayList<LoadBalancer>();  //　LBのリスト	
		// インスタンス1_1
		Instance instance1_1 = new Instance(1, "amazonVM1", "t1.micro", 1, 2, 100, "SSD","CentOS6","NorthAmerica_US_Virginia", "amazon",0.33);
		list1_1.add(instance1_1);  //追加
		Plan plan1=new Plan(list1_1,list1_2);
		planList.add(plan1);
		
		// プラン2(GoogleのVM一つ)
		ArrayList<Instance> list2_1=new ArrayList<Instance>();  // インスタンスのリスト
		ArrayList<LoadBalancer> list2_2=new ArrayList<LoadBalancer>();  //　LBのリスト	
		// インスタンス2_1
		Instance instance2_1 = new Instance(2, "GoogleVM1","f1-micro",1,2,0,"SSD","CentOS6","NorthAmerica_US_central1","google",0.25);
		list2_1.add(instance2_1);  // 追加
		Plan plan2=new Plan(list2_1,list2_2);
		planList.add(plan2);
		
		// プラン3（amazonのLB一つ）
		ArrayList<Instance> list3_1=new ArrayList<Instance>();  // インスタンスのリスト
		ArrayList<LoadBalancer> list3_2=new ArrayList<LoadBalancer>();  //　LBのリスト	
		// LB3_1
		LoadBalancer lb3_1=new LoadBalancer(1,"amazonLB1","ElasticLoadBalancing","amazon","NorthAmerica_US_Virginia","NA");
		list3_2.add(lb3_1);
		Plan plan3=new Plan(list3_1,list3_2);
		planList.add(plan3);
		
		//プラン４（GoogleのLB一つ）
		ArrayList<Instance> list4_1=new ArrayList<Instance>();  // インスタンスのリスト
		ArrayList<LoadBalancer> list4_2=new ArrayList<LoadBalancer>();  //　LBのリスト	
		// LB4_1
		LoadBalancer lb4_1=new LoadBalancer(1,"GoogleLB1","NetworkLoadBalancing","google","NorthAmerica_US_central1","NA");
		list4_2.add(lb4_1);
		Plan plan4=new Plan(list4_1,list4_2);
		planList.add(plan4);
		
		//プラン５（amazonのVM二つ、LB一つ）
		ArrayList<Instance> list5_1=new ArrayList<Instance>();  // インスタンスのリスト
		ArrayList<LoadBalancer> list5_2=new ArrayList<LoadBalancer>();  //　LBのリスト	
		// インスタンス5_1
		Instance instance5_1 = new Instance(1, "amazonVM1", "t1.micro", 1, 2, 100, "SSD","CentOS6","NorthAmerica_US_Virginia", "amazon",0.33);
		Instance instance5_2 = new Instance(2, "amazonVM2", "t1.micro", 1, 2, 100, "SSD","CentOS6","NorthAmerica_US_Virginia", "amazon",0.33);
		list5_1.add(instance5_1);
		list5_1.add(instance5_2);
		LoadBalancer lb5_1=new LoadBalancer(1,"amazonLB1","ElasticLoadBalancing","amazon","NorthAmerica_US_Virginia","1,2");
		list5_2.add(lb5_1);
		Plan plan5=new Plan(list5_1,list5_2);
		planList.add(plan5);
		*/
		
		return planList;
	}

	


	
}
