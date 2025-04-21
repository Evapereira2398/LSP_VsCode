package custom.senior.apuracao;

import java.util.List;

import org.joda.time.LocalDate;

import com.senior.ContextoGeralRH;
import com.senior.dataset.ICursor;
import com.senior.dataset.IEntity;
import com.senior.dataset.MappedParamProvider;
import com.senior.rh.Colaborador;
import com.senior.rh.entities.readonly.IR002FEC;
import com.senior.rh.entities.readonly.IR003DTF;
import com.senior.rh.entities.readonly.IR066SIT;
import com.senior.rh.ponto.apuracao.calculo.TipoHoraExtra;
import com.senior.rh.ponto.apuracao.calculo.TipoIntervalo;
import com.senior.rh.ponto.colaborador.Compensacao;
import com.senior.rh.ponto.colaborador.Horario;
import com.senior.rh.ponto.marcacoes.MarcacaoRegra;
import com.senior.rule.Rule;

import br.com.senior.gp.da.entity.apuracao.MensagemApuracao;
import custom.senior.apuracao.Apuracao;
import custom.senior.apuracao.ContextoApuracao;


@Rule(description = "Regra de Apuração Diária.")
public class RegraApuracao extends Apuracao {	

	public void execute() {
		
		// Busca os contextos
		ContextoApuracao apuracao = getContainer().getContextoApuracao();
		Colaborador colaborador = apuracao.getColaborador();
		ContextoGeralRH geral = getContainer().getContextoGeral();
		Horario horario = apuracao.getHorario();
		
		// Busca dados do colaborador e dia em processamento
		int numEmp = colaborador.getNumEmp();
		int tipCol = colaborador.getTipCol();
		int numCad = colaborador.getNumCad();		
		LocalDate datPro = apuracao.getData();
		LocalDate iniApu = apuracao.getDataInicial();
		LocalDate fimApu = apuracao.getDataFinal();		
		int codHor = horario.getCodigo();
		List<MarcacaoRegra> listaMarcacoes = apuracao.getMarcacoesRealizadas(true);
		
		// Inicialização variáveis
		MappedParamProvider paramProvider = new MappedParamProvider();
		int diaFer = 0;
		int diaFac = 0;
		int horExt = 0;
		
		// Busca marcações realizadas de entrada e saída
		int priMar = 0;
		int ultMar = 0;
		for (int i = 0; i < listaMarcacoes.size(); i++) {
			if (i == 0)
				priMar = listaMarcacoes.get(i).getHora(); // primeira marcação do dia
			else
				ultMar = listaMarcacoes.get(i).getHora(); // última marcação do dia
		}
		
		// Busca marcações previstas de entrada e saída
		int priPrv = 0;
		int ultPrv = 0;
		for (int i = 0; i < horario.getMarcacoes().size(); i++) {
			if (i == 0)
				priPrv = horario.getMarcacoes().get(i).getMinutos(); // primeira marcação prevista
			else
				ultPrv = horario.getMarcacoes().get(i).getMinutos(); // última marcação prevista
		}
		
		// Verifica se é um feriado
		paramProvider.setParam("datPro", datPro);
		IR002FEC c002Fec = consulta(IR002FEC.class, "CodFer=2 AND DatFer=:datPro", paramProvider);
		if (c002Fec != null) {
			diaFer = 1;
		}				
		
		// Verifica se é um dia facultativo
		paramProvider.setParam("datPro", datPro);
		IR003DTF c003Dtf = consulta(IR003DTF.class, "CodDFa=1 AND DatDFa=:datPro", paramProvider);
		if (c003Dtf != null) {
			diaFac = 1;
		}			
		
		
		// Verificação limite BH total positivo e negativo
		int limPos = 2400;        // 40hrs positivas
		int limNeg = 1080 * (-1); // 18hrs negativas 
		int salBan = apuracao.getSaldoBanco(1, numEmp, tipCol, numCad, datPro);
		int totBan = salBan + apuracao.getHorSit(907) + apuracao.getHorSit(911) - apuracao.getHorSit(908);
		
			
		// Se o total do banco for maior que 40hrs positivas
		if (totBan > limPos) {
			
			int difBan = totBan - limPos;
			
			if (apuracao.getHorSit(911) >= difBan) { //Se BH Noturno excederem o Limite
				apuracao.setHorSit(911, apuracao.getHorSit(911) - difBan);
				apuracao.setHorSit(302, apuracao.getHorSit(302) + difBan);
			} else if (apuracao.getHorSit(907) >= difBan) { // Se BH Diurno excederem o limite
				apuracao.setHorSit(907, apuracao.getHorSit(907) - difBan);
				apuracao.setHorSit(301, apuracao.getHorSit(301) + difBan);
			} else if ((apuracao.getHorSit(907)) > 0 && (apuracao.getHorSit(907) < difBan)) { // Se BH Noturno não excederem o Limite
				int difExt = difBan - apuracao.getHorSit(907);
				apuracao.setHorSit(301, apuracao.getHorSit(301) + apuracao.getHorSit(907));
				apuracao.setHorSit(907, 0);
				
				if (difExt <= apuracao.getHorSit(911)) {
					apuracao.setHorSit(302, difExt);
					apuracao.setHorSit(911, apuracao.getHorSit(302) - difExt);
				}
			}
		} 
		
		// Se o total do banco for menor que 18hrs negativas
		else if (totBan < limNeg) {			
			int difBan = totBan - limNeg; 
			difBan = difBan * (-1);
			
			if (apuracao.getHorSit(908) > difBan){ 
				apuracao.setHorSit(908, apuracao.getHorSit(908) - difBan);
			}
			else {
				apuracao.zeraHorasSituacao(908);						
			}
			
			
			// tratamento atraso
			if (priMar > priPrv && (priMar - priPrv) < 60) {
				if (priMar < 300)
					apuracao.setHorSit(104, priMar - priPrv);
				else
					apuracao.setHorSit(103, priMar - priPrv);
			}
			
			
			// tratamento saída antecipada
			if (ultMar < ultPrv && (ultPrv - ultMar) < 60) {
				if (priMar < 300){
					apuracao.setHorSit(102, ultPrv - ultMar);
				}
				else {
					apuracao.setHorSit(101, ultPrv - ultMar);
				}
					
			}
			
		}
				
		
		// É calculado para os funcionários as horas extras da seguinte forma, as 10 primeiras horas semanais são pagas com adicional de 50%, e o restante com adicional de 90%
		if (codHor != 9996 && codHor != 9997 && codHor != 9999 && diaFer == 0 && diaFac == 0) {					
			
			// Definição da Data Inicial de acordo com o dia da Semana 
			LocalDate datIni = LocalDate.parse("1900-12-31");	
			if (geral.getDiaSem(datPro) == 2) { 	 // Terça-feira
				datIni = datPro.minusDays(1);
			} else if (geral.getDiaSem(datPro) == 3) { // Quarta-feira
				datIni = datPro.minusDays(2);
			} else if (geral.getDiaSem(datPro) == 4) { // Quinta-feira
				datIni = datPro.minusDays(3);
			} else if (geral.getDiaSem(datPro) == 5) { // Sexta-feira
				datIni = datPro.minusDays(4);
			} else if (geral.getDiaSem(datPro) == 6) { // Sábado
				datIni = datPro.minusDays(5);
			} 
			
			// Cursor para buscar as Horas Extras da Semana
			if (geral.getDiaSem(datPro) > 1) {
				MappedParamProvider paramsR066 = new MappedParamProvider();
				paramsR066.setParam("numEmp", numEmp);
				paramsR066.setParam("tipCol", tipCol);
				paramsR066.setParam("numCad", numCad);	
				paramsR066.setParam("datIni", datIni);
				paramsR066.setParam("datPro", datPro);									
				try {
					ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession().newCursor(IR066SIT.class);
					cCursor_R066.addFilter("NumEmp = :numEmp AND "
										 + "TipCol = :tipCol AND "
										 + "NumCad = :numCad AND "
										 + "CodSit in (301,302,307,308) AND "
										 + "DatApu >= :datIni AND "
										 + "DatApu < :datPro",	paramsR066);

					try {
						cCursor_R066.open();
						while (cCursor_R066.next()) {
							IR066SIT eR066Sit = cCursor_R066.read();										
							horExt = horExt + eR066Sit.getQtdHor();						
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						cCursor_R066.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
			// Tratamento para separar corretamente HE noturna no ínicio do expediente				
			if (listaMarcacoes.size() > 0)
				priMar = listaMarcacoes.get(0).getHora();			
			if (priMar < 300 && listaMarcacoes.size() > 0 && codHor != 9996 && codHor != 9997 && codHor != 9998 && codHor != 9999) {
				if (priMar == 0) {				
					if (apuracao.getHorSit(302) > 0) {
						apuracao.setHorSit(302, 300);
						apuracao.setHorSit(301, apuracao.getHorSit(301) - 60);
					} else
						apuracao.setHorSit(302, apuracao.getHorSit(302) + 300);
				} else {						
					apuracao.setHorSit(301, apuracao.getHorSit(301) - (300 - priMar) + apuracao.getHorSit(302));
					apuracao.setHorSit(302, 300 - priMar);
				}
			}			
			
			
			// Total de Horas Extras, incluindo o Dia do Processamento 
			int totExt = horExt + apuracao.getHorSit(301) + apuracao.getHorSit(302);
			
			
			// Total de Extras maior que o Limite e as Horas Extras da Semana Menor que o Limite 
			if ((totExt > 600) && (horExt < 600)) {
				// Horas que excederam o limite 
				int excLim = totExt - 600;
				
				if (apuracao.getHorSit(302) >= excLim) { //Se as Horas Extras Noturnas excederem o Limite
					apuracao.setHorSit(302, apuracao.getHorSit(302) - excLim);
					apuracao.setHorSit(308, excLim);
				} else if (apuracao.getHorSit(301) >= excLim) { // Se as Horas Extras Diurnas excederem o limite
					apuracao.setHorSit(301, apuracao.getHorSit(301) - excLim);
					apuracao.setHorSit(307, excLim);
				} else if ((apuracao.getHorSit(301)) > 0 && (apuracao.getHorSit(301) < excLim)) { // Se as Horas Extras Noturnas não excederem o Limite
					int difExt = excLim - apuracao.getHorSit(301);
					apuracao.setHorSit(307, apuracao.getHorSit(301));
					apuracao.setHorSit(301, 0);
					
					if (difExt <= apuracao.getHorSit(302)) {
						apuracao.setHorSit(308, difExt);
						apuracao.setHorSit(302, apuracao.getHorSit(302) - difExt);
					}
				}
			} else if (totExt >= 600 && horExt >= 600) {
				apuracao.setHorSit(307, apuracao.getHorSit(301));
				apuracao.setHorSit(308, apuracao.getHorSit(302));
				apuracao.setHorSit(301, 0);
				apuracao.setHorSit(302, 0);
			}
			
			
		} else { // Domingos, feriados e dias pontes compensados até 8h = 100% e acima = 150%			
			apuracao.zeraHorasSituacao(907);
			apuracao.zeraHorasSituacao(911);	
			
			// Tratamento para compensação de feriado
			if (codHor == 9998 && (diaFer == 1 || diaFac == 1)) {
				
				int totExt = apuracao.getHorSit(301) + apuracao.getHorSit(302);
				if (totExt > 480) {												
					// Horas que excederam o limite 
					int excLim = totExt - 480;
					
					if (apuracao.getHorSit(302) >= excLim) { //Se as Horas Extras Noturnas excederem o Limite
						apuracao.setHorSit(304, apuracao.getHorSit(302) - excLim);
						apuracao.setHorSit(316, excLim);
					} else if (apuracao.getHorSit(301) >= excLim) { // Se as Horas Extras Diurnas excederem o limite
						apuracao.setHorSit(303, apuracao.getHorSit(301) - excLim);
						apuracao.setHorSit(313, excLim);
					} 					
				} else {
					apuracao.setHorSit(303, apuracao.getHorSit(301));
					apuracao.setHorSit(304, apuracao.getHorSit(302));
				}	
				apuracao.zeraHorasSituacao(301);
				apuracao.zeraHorasSituacao(302);
			}
		}
		
		// Reflexo adicional noturno	
		apuracao.somaHorasSituacao(70, apuracao.getHorSit(302) + apuracao.getHorSit(304) + apuracao.getHorSit(308) + apuracao.getHorSit(316) + apuracao.getHorSit(911));
				
	}
	
	// Método genérico para fazer um cursor
		private <C extends IEntity> C consulta(Class<C> entidade, String filtro, MappedParamProvider paramProvider) {
			ICursor<C> cursor = getContainer().getEntitySession().newCursor(entidade);
			if (filtro != "")
				cursor.addFilter(filtro, paramProvider);
			cursor.open();
			try {
				if (cursor.first()) {
					C ler = cursor.read();
					return ler;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				cursor.close();
			}
			return null;
		}

}
