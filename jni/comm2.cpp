// comm.cpp : 定义控制台应用程序的入口点。
//

//#include "stdafx.h"
#include "stdio.h"
#include "stdlib.h"
#include "string.h"

//#include "platform.h"

#define BARCODE_Y_INIT_THRESHOLD	2000

#define BARCODE_LENGTH_MAX		320
typedef struct audioInterface_handle_t
{
	jbyte		signal;		/* 表示当前是在通讯区还是监听区*/
	jbyte		polarity;	/* 耳机的极性，决定了上升沿是1，还是下降沿为1，由初始化程序分析完成*/

	jbyte		interval_lowerlimit;	/* 边缘的间隔的下限*/
	jbyte		interval_toplimit;	/* 边缘的间隔的上限*/

	int		x__2;		/* x(-2)  在监听区时，是原始值，在数据区时，是滤波值*/
	int		x__1;		/* x(-1)  在监听区时，是原始值，在数据区时，是滤波值
									由于滤波后可能超出S16范围，所以必须使用int类型*/

	float	sum_n_1_2;	/* iir滤波计算的中间值*/
	float	sum_n_2;

	int		y__3;				/*  一次导数  x'(i-3) */
	int		y__2;				/*  一次导数  x'(i-2) */

	int		y_threshold;		/* 一次导数的极值的阈值*/
	int		idx_ypeak_last;		/* 上一个一次导数极值点的idx坐标*/

#ifdef WIN32
	int		end;
#endif

	jbyte		rev_bit_idx;		/* bit idx*/
	jbyte		rev_byte_idx;		/* byte idx*/
	jbyte		rev_data[BARCODE_LENGTH_MAX];		/* 暂存的接受的数据*/

	jchar		rev_bytes;			/* 接受的字节数*/

}tAUDIO_INTERFACE_HANDLE;
int polarity = 1;
static tAUDIO_INTERFACE_HANDLE  gtAudioInterface;
int audioInterface_wav2digital(jshort *x, int n, jbyte *pdata);
/*-------------------------------------------------------------------------
* 函数:	audioInterface_init
* 说明:	手机通过音频口的mic，得到波形，然后通过分析波形得到数据。这里对全局变量
*		进行初始化。输入采样率，和一组对应8bit的0 的wave数据，分析出之后解析数据
*		需要的
* 参数:	无
* 返回:	0  正确
*		-1 参数错误
* ------------------------------------------------------------------------*/
int audioInterface_init(int sample_rate, int data_rate)
{
	tAUDIO_INTERFACE_HANDLE *pai;
	int i, bytes;
	jbyte	data[32];	/* 初始化数据，最多包含32个字节 */

	pai = &gtAudioInterface;
	memset(pai,0,sizeof(tAUDIO_INTERFACE_HANDLE)); 

	if(sample_rate == 44100)
	{
		/* 每个周期约11.1个点 */
		pai->interval_lowerlimit= 9;
		pai->interval_toplimit	= 16;	/* 约1.5个周期*/


	}
	else if(sample_rate == 48000)
	{
		/* 每个周期约12个点 */
		pai->interval_lowerlimit= 9;
		pai->interval_toplimit	= 15;	/* 约1.5个周期*/

	}
	else
	{
		return -1;  /* 参数错误 */
	}

	pai->y_threshold	= BARCODE_Y_INIT_THRESHOLD;

	/* 由于多数手机收到的波形和反向的，即上升沿代表了0，下降沿代表1，所以初始值的极性设为0*/
	pai->polarity		= polarity;

	return 0;	/* 初始化成功*/
	
}


/*-------------------------------------------------------------------------
* 函数:	audioInterface_wav2digital
* 说明:	分析mic的adc数据，解析出数字信号
* 参数:	x	-- in，wave 数据的PCM样本，jshort 类型
*		n	-- in, wave 数据PCM样本的个数， 输入大小和下位机协商，
*				保证一次最多输出一个条码数据
*		pdata -- out  解析出的数据的缓存buf
* 返回:	接收的数据的字节数，如果是多段通讯数据，则中间插入0，返回为值
		通讯的字节数+ (段数-1)
* ------------------------------------------------------------------------*/
int audioInterface_wav2digital(jshort *x, int n, jbyte *pdata)
{
	/* 低通滤波器，低截止频率0.1 */
	const float a[2] ={-1.14298, 0.412802};		
	const float b[3] ={0.0674553, 0.134911, 0.0674553};

	int		i,j;
	int		xraw,idx_ypeak,y__1, interval;
	float	x_filter;

	tAUDIO_INTERFACE_HANDLE *pai;

	pai = &gtAudioInterface;

	/* 最外圈的while循环， 用来控制 监听区和通讯区的两种状态的切换
	 在通讯区进行滤波，使用滤波值计算导数，一次导数的求取，在点上是连续的 */
	i = 0;
	while(i<n)  
	{
		if(pai->signal)  /* 通讯区 */
		{
			while(i<n)  /* 使用while 逐点处理 */
			{
				/* 先对x进行逐点滤波, 以去除噪音*/
				x_filter		= (b[0]*(x[i])+ pai->sum_n_1_2);
				pai->sum_n_1_2	= pai->sum_n_2 +b[1]*x[i] -a[0]*x_filter ;
				pai->sum_n_2	= b[2]*x[i] -a[1]* x_filter;

				/* 由于x滤波值可能超出U16的范围 -32768~32767， 所以，存在int类型的变量中
				下面使用滤波值计算一次导数， 中心法 即 x'(i-1) = x(i)- x(i-2)*/
				y__1 = x_filter - pai->x__2;

				interval = 0;
				/* 注意以下判断，当遇到连续几个一次导数一样的情况，取最左边的点，所以需要 y__2和y__3 不等*/
				if( (pai->y__2 > pai->y_threshold	&& pai->y__2 >= y__1 && pai->y__2 > pai->y__3) ||
					(pai->y__2 <(-pai->y_threshold) && pai->y__2 <= y__1 && pai->y__2 < pai->y__3))
				{

					/* 更新阈值, 仅在导数为正的时候更新 */
					if(pai->y__2> (2 * BARCODE_Y_INIT_THRESHOLD))
					{
						pai->y_threshold = pai->y__2/2;
					}

					/* 找到了一次导数极值，判断是否是数据 */
					idx_ypeak	= i-2;
					interval	= idx_ypeak - pai->idx_ypeak_last;

					if(interval > pai->interval_toplimit)
					{
						/* 当前沿和之前的沿的距离大于一个周期, 需要处理以下步骤
						1. 如果之前已经产生过沿，则认为是之前的数据结束了，‘
							如果之前得到过整字节数的数据，则认为数据有效根据情况处理；
						2. 从当前沿开始，又一个通讯周期开始*/
						if(pai->rev_byte_idx >=1 && pai->rev_bit_idx ==0 )
						{
							/* 至少产生了整字节的数据，才算有效, 记录，否则就忽略*/
							memcpy(&pdata[pai->rev_bytes],pai->rev_data, pai->rev_byte_idx);
							pai->rev_bytes			+=pai->rev_byte_idx;
							pdata[pai->rev_bytes]	= 0;	/* 结尾插入0 */
							pai->rev_bytes++;
						}

						/*重新开始通讯, 注意rev_data一定要清零，因为下面的逻辑是bit为1时或，bit为0时不变*/
						memset(pai->rev_data,0,BARCODE_LENGTH_MAX); 
						pai->rev_byte_idx	= 0;
						pai->rev_data[0]	= (pai->polarity)?((pai->y__2>0)?1:0):((pai->y__2<0)?1:0);  /* 第一个数据，bit0 的数据*/
						pai->rev_bit_idx	= 1;

						pai->idx_ypeak_last = idx_ypeak;

					}
					else if(interval >= pai->interval_lowerlimit)
					{
						/* 记录该沿代表的bit*/
						if(pai->polarity &&pai->y__2>0 )
						{
							pai->rev_data[pai->rev_byte_idx]	|= (1<<(pai->rev_bit_idx));  
						}
						else if(pai->polarity==0 && pai->y__2<0 )
						{
							pai->rev_data[pai->rev_byte_idx]	|= (1<<(pai->rev_bit_idx));  
						}



						if(pai->rev_bit_idx == 7)
						{
							pai->rev_bit_idx = 0;
							pai->rev_byte_idx ++;
							if(pai->rev_byte_idx == 32)
							{
								/* 溢出处理 */
							}
						}
						else
						{
							pai->rev_bit_idx++;
						}

						pai->idx_ypeak_last = idx_ypeak;

					}
					/* else 和上个沿距离不够，忽略该沿，因为该沿是通讯电平复位造成的,忽略*/

				}
				else if( (pai->rev_bit_idx|pai->rev_byte_idx) &&
						 (i-2 - pai->idx_ypeak_last) >(pai->interval_toplimit<<2) /* 恢复进入监听区的宽度增大*/)
				{
					/* 产生过边缘后，又有一段距离没有边缘产生，认为是通讯区结束*/
					if(pai->rev_byte_idx >=1 && pai->rev_bit_idx ==0 )
					{
						/* 至少产生了整字节的数据，才算有效, 记录，否则就忽略*/
						memcpy(&pdata[pai->rev_bytes],pai->rev_data, pai->rev_byte_idx);
						pai->rev_bytes			+= pai->rev_byte_idx;
						pdata[pai->rev_bytes]	= 0;	/* 结尾插入0 */
						pai->rev_bytes++;
						
						/* 注意由于rev_data 的逻辑是 bit = 1 或， bit0 不变，所以必须清零*/
						memset(pai->rev_data,0,BARCODE_LENGTH_MAX); 

						

#ifdef WIN32
						pai->end = i;
#endif
					}

					/*重新开始准备通讯区，break后，转到监听区*/
					pai->rev_byte_idx	= 0;
					pai->rev_bit_idx	= 0;

					i++;/* break，调整idx */
					pai->signal = 0;
					break;
				}

				/* 更新滤波值*/
				pai->x__2 = pai->x__1;
				pai->x__1 = x_filter;

				/* 更新y__3 和 y__2 */
				pai->y__3 = pai->y__2;
				pai->y__2 = y__1;

				i++;	/* 最后更新索引，是为了上面的逻辑中，i仍是当前索引 */
			}
		}

		/*	上面通讯区转到监听区，即搜索数据开始的状态，
		    这里需要重新判断，是因为上面通讯区可能会改变这个状态*/
		if(pai->signal == 0)  
		{
			/* 监听搜索区恢复初始阈值 */
			pai->y_threshold	= BARCODE_Y_INIT_THRESHOLD;

			while(i<2)
			{
				y__1 = x[i] - ((i == 0)?pai->x__2:pai->x__1);
				if(y__1 > pai->y_threshold || y__1 < (-pai->y_threshold) )
				{
					pai->signal		= 1;/* 得到一个上升沿或者下降沿 */
					break;
				}
				i++;
			}

			if(pai->signal == 0)
			{
				while(i<n)  /* 使用while 逐点处理 */
				{
					y__1 = x[i]- x[i-2];
					if(y__1 > pai->y_threshold || y__1 < (-pai->y_threshold) )
					{
						pai->signal			= 1;/* 得到一个上升沿或者下降沿 */
						break;
					}
					i++;	/* 最后更新索引，是为了上面的逻辑中，i仍是当前索引 */
				}
			}

			/* 准备进入数据解析区，为了减少误差，先准备iir滤波的初值*/
			if(pai->signal == 1)
			{

				/* 在数据解析区，对原始数据进行iir滤波，这里假定 x__2 之前有16个数据一样，
				以便减少iir开始的sum的累计误差
				而且，由于iir有滞后性，所以，应从x(i-2) 开始计算iir的滤波值，否则第一个
				极值点更接近于没有滤波前的数据，从而引起之后的距离误差*/
				pai->sum_n_1_2	= 0;
				pai->sum_n_2	= 0;

				/* 从x(i-2)开始计算滤波值 */
				xraw = (i>=2)? x[i-2]:((i==0)? pai->x__2: pai->x__1);
				for(j=0;j<16;j++)
				{
					x_filter		= (b[0]*(xraw)+ pai->sum_n_1_2);
					pai->sum_n_1_2	= pai->sum_n_2 +b[1]*xraw -a[0]*x_filter ;
					pai->sum_n_2	= b[2]*xraw -a[1]* x_filter;
				}

				/* 存储i-2 点的滤波值并 计算 i-1 点的滤波值
					注意为了防止溢出，需要存入到int类型的变量中
					且后续都是使用 x__2 和 x__1 计算，这里更新相对于当前点的x__2 和x__1的值*/
				pai->x__2	= x_filter;

				xraw			= (i>=1)? x[i-1]:pai->x__1;
				pai->x__1		= (b[0]*xraw+ pai->sum_n_1_2);
				pai->sum_n_1_2	= pai->sum_n_2 +b[1]*xraw -a[0]*x_filter ;
				pai->sum_n_2	= b[2]*xraw -a[1]* x_filter;

				/* 初始化一次导数的历史值，初始化为0即可，因为这时候总认为不在极值点处*/
				pai->y__3 = 0;
				pai->y__2 = 0;


			}

		} /* 非通讯区（搜索信号开始）的判断*/

	} /* 最外圈的while，控制状态切换*/


	pai->idx_ypeak_last -=n;	/* 更新 idx 索引*/

//	if (pai->rev_bytes == 7)
//	{
//		if ( pdata[0] == 0xBF && pdata[1] == 0xFD && pdata[3] == 0x5F)
//		{
//			polarity = 1;
//			gtAudioInterface.polarity = polarity;
//		}else{
//			polarity = 0;
//			gtAudioInterface.polarity = polarity;
//		}
//	}

	return  pai->rev_bytes;

}

/* 应用已经使用了 清为0*/
void audioInterface_clearCount(void)
{
	gtAudioInterface.rev_bytes = 0;

}

/* 初始化检测w200时，需要设置极性*/
void audioInterface_reversePolarity(int polarity)
{
	gtAudioInterface.polarity = polarity;
}
#if 0
/*------------------------------- pc test ----------------------------*/

#ifdef WIN32
void audioInterface_get_comm_start(int *end)
{
	*end	= gtAudioInterface.end;
}
#endif


/* 将bin 文件数据读入到一个sample的数组中，返回sample数 */
int adc_binfile2Samples( char *filename, jchar **ppsamples)
{
	int i, length_buf, sample_num;
	jbyte *pbuf;
	jchar *psample;
	FILE *fp;

	if((fp = fopen(filename,"rb"))== NULL)
	{
		return -1;
	}

	if((pbuf = (jbyte*)malloc(5*1024*1024))== NULL)
	{
		return -1;
	}
	length_buf = fread(pbuf,1,5*1024*1024,fp);
	fclose(fp);

	psample		=  (jchar*)pbuf;
	sample_num	= length_buf/2;/* 字节数 -> jchar */

	*ppsamples = psample;

	return sample_num;  
}


typedef struct comm_record_t
{
	int end;
	int length;
	jbyte	data[36];
}tCOMM_RECORD;

#define COMM_REC_NUM  1024
tCOMM_RECORD gtcomm_rec[COMM_REC_NUM];

#define SAMPLES_PER_BLOCK	2000

#define TEST_WAV_FILENAME	"E:\\labview\\mic_wave\\魅族M4_徐非凡.wav"//"E:\\labview\\mic_wave\\三星note2_王志特.wav"


int _tmain(int argc, _TCHAR* argv[])
{
	int i,j,gadcSamples;
	int rec_idx, start, end;
	int bytes;
	short *padc_data;
	FILE *fp;

	memset((char*)gtcomm_rec,0,sizeof(tCOMM_RECORD)*COMM_REC_NUM);  


	gadcSamples = adc_binfile2Samples(TEST_WAV_FILENAME, (jchar**) &padc_data);
	
	/* 初始化时，需要给出一串1 的数据，以便判断手机是否可用，上升沿和下降沿的极性*/
	audioInterface_init(44100,4000,padc_data,2000 );

	rec_idx = 0;
	for(i = 44; i< gadcSamples; i=i+SAMPLES_PER_BLOCK)
	{
		bytes = 0;
		bytes = audioInterface_wav2digital((jshort*)&padc_data[i], SAMPLES_PER_BLOCK, (jbyte*)gtcomm_rec[rec_idx].data);
		if(bytes)
		{
			audioInterface_get_comm_start(&end);
			gtcomm_rec[rec_idx].end		= i+end;
			gtcomm_rec[rec_idx].length	= bytes;

			audioInterface_clearCount();

			rec_idx ++;
			if(rec_idx >= COMM_REC_NUM)
			{
				break;	
			}

			//audioInterface_init(44100,4000);
		}
	}

	free(padc_data);

	/* 输出结果到一个文件中*/
	fp = fopen("E:\\W200\\comm\\result.txt","wb");
	if(fp == NULL)
	{
		return 0;
	}

	fprintf(fp, "filename = %s\n",  TEST_WAV_FILENAME );
	for(i =0; i< 500; i++)
	{
		fprintf(fp, "%4d   end= %-8d  ", i,gtcomm_rec[i].end);

		for(j= 0; j< gtcomm_rec[i].length; j++)
		{
			fprintf(fp, "%-2x ",gtcomm_rec[i].data[j]);
		}
		fprintf(fp, "\n");
	}

	fclose(fp);

	while(1);


	return 0;
}



#if  0 //  初始化过程中，判断通讯极性的例子
	memset(data,0,32);
	bytes = audioInterface_wav2digital(x, n, data);

	/* 检查data 中的数据*/
	if(bytes)
	{
		if(data[0] == 0x0/* 下降沿表示1的情况*/ || data[0] == 0xFF /* 上升沿表示1的情况*/)
		{
			for(i = 1; i<bytes; i++)
			{
				if(data[i] != data[0])
				{
					return -1;
				}
			}

			if(data[0] == 0xFF)
			{	
				audioInterface_reversePolarity();
			}
		}

		
	}
	else
	{
		return -1;	
	}

#endif
#endif
