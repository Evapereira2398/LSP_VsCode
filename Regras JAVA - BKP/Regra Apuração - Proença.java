package custom.proenca.apuracao;

import java.util.List;

import java.time.LocalDate;         // Fonte convertido do joda. Linha original: import org.joda.time.LocalDate;

import com.senior.ContextoGeralRH;
import com.senior.dataset.ICursor;
import com.senior.dataset.IEntity;
import com.senior.dataset.MappedParamProvider;
import com.senior.rh.Colaborador;
import com.senior.rh.entities.readonly.IR006ESC;
import com.senior.rh.entities.readonly.IR010SIT;
import com.senior.rh.entities.readonly.IR010TOB;
import com.senior.rh.entities.readonly.IR014SIN;
import com.senior.rh.entities.readonly.IR030EMP;
import com.senior.rh.entities.readonly.IR066SIT;
import com.senior.rh.entities.readonly.IR070ACC;
import com.senior.rh.ponto.apuracao.ApuracaoException;
import com.senior.rh.ponto.apuracao.calculo.TipoIntervalo;
import com.senior.rh.ponto.colaborador.Compensacao;
import com.senior.rh.ponto.colaborador.HistoricoAfastamento;
import com.senior.rh.ponto.marcacoes.MarcacaoRegra;
import com.senior.rule.Rule;

import custom.senior.apuracao.Apuracao;
import custom.senior.apuracao.ContextoApuracao;

@Rule(description = "Regra customizada de apuração")
public class RegraApuracao extends Apuracao {
	@Override
	public void execute() {
		int iCodHor;
		int iCodSin;
		int iHorReaDiu = 0;
		int iHorReaNot = 0;
		int iHorPreDiu = 0;
		int iHorPreNot = 0;
		int iIntMin4h = 0;
		int iIntMin6h = 0;
		int iCodDsi = 0;
		long lHorDsr = 0;
		int iSitTra = 0;
		int iSitAdi = 0;
		int iSitNId = 0;
		int iSitFal = 0;
		int iNorAnt = 0;
		int iNorFer = 0;
		int iSitVIn = 0;
		int iHorTra = 0;
		int iHorInt = 0;
		int iHorRef = 0;
		int iFatRed = 0;
		int iHExt60 = 0;
		int iTotExt = 0;
		int iDiaTra = 0;
		int iTotTob = 0;
		int iSitTraCmp = 0;
		int iNorAntCmp = 0;
		int iBatInt = 0;
		boolean bEntrou = false;
		boolean bTem21 = false;
		LocalDate dDatIni;
		LocalDate dDatFim;
		LocalDate dDatAux;

		MappedParamProvider paramProvider = new MappedParamProvider();
		ContextoApuracao apuracao = getContainer().getContextoApuracao();
		ContextoGeralRH geral = getContainer().getContextoGeral();
		Colaborador colaborador = apuracao.getColaborador();
		iCodSin = apuracao.getHistoricoSindicato().getCodSin(); 
		iCodHor = apuracao.getHorario().getCodigo();
		lHorDsr = apuracao.getHistoricoEscala().getHorDsr();
		iHorReaDiu = apuracao.getHorasTrabalhadas().getHorasDiurnas()
				+ apuracao.getHorasSeparadas(TipoIntervalo.EXTRA).getHorasDiurnas();
		iHorReaNot = apuracao.getHorasTrabalhadas().getHorasNoturnas()
				+ apuracao.getHorasSeparadas(TipoIntervalo.EXTRA).getHorasNoturnas();
		iHorPreDiu = apuracao.getTotalMinutosPrevisto(iCodHor).getHorasDiurnas();
		iHorPreNot = apuracao.getTotalMinutosPrevisto(iCodHor).getHorasNoturnas();
		iFatRed = geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
				colaborador.getNumCad(), apuracao.getData()).getFatorHorasJornada();
		dDatIni = apuracao.getData().minusDays(6);
		dDatFim = apuracao.getData();
		dDatAux = LocalDate.parse("2018-01-01");
		
		MappedParamProvider paramProvider3 = new MappedParamProvider();
		paramProvider3.setParam("dDatIni", dDatIni);
		paramProvider3.setParam("dDatFim", dDatFim);
		paramProvider3.setParam("iNumEmp", colaborador.getNumEmp());
		paramProvider3.setParam("iTipCol", colaborador.getTipCol());
		paramProvider3.setParam("iNumCad", colaborador.getNumCad());

		// 7 DIAS TRABALHADOS
		ICursor<IR066SIT> c066Sit = consulta2(IR066SIT.class,
				"DatApu >= :dDatIni And DatApu <= :dDatFim And CodSit IN (1,51, 307, 308, 309, 357, 358, 409) And NumEmp = :iNumEmp And TipCol = :iTipCol And NumCad = :iNumCad ",
				"DatApu", paramProvider3);

		try {
			IR066SIT ir066sit = c066Sit.newBuffer();
			while (c066Sit.next()) {
				c066Sit.read(ir066sit);
				if (dDatAux.equals(LocalDate.parse("2018-01-01"))
						|| (dDatAux.isBefore(ir066sit.getDatApu()) || dDatAux.isAfter(ir066sit.getDatApu()))) {
					iDiaTra++;
					dDatAux = ir066sit.getDatApu();
				}
			}
		} finally {
			c066Sit.close();
		}

		if (iDiaTra >= 7) {
			try {
				apuracao.criarAlerta(13);
			} catch (ApuracaoException e) {

			}
		}

		// DOMINGOS TRABALHADOS
		int idia = geral.getDiaSem(apuracao.getData());
		if (idia == 7) {
			dDatAux = LocalDate.parse("2018-01-01");
			iDiaTra = 0;
			MappedParamProvider paramProvider4 = new MappedParamProvider();
			paramProvider4.setParam("dDat1", apuracao.getData()); // data atual
			paramProvider4.setParam("dDat2", apuracao.getData().minusDays(7));
			// 14 dias antes, porém aqui deve-se limitar aos domingos do periodo de apuração
			paramProvider4.setParam("dDat3", apuracao.getData().minusDays(14)); 
			paramProvider4.setParam("iNumEmp", colaborador.getNumEmp());
			paramProvider4.setParam("iTipCol", colaborador.getTipCol());
			paramProvider4.setParam("iNumCad", colaborador.getNumCad());
			ICursor<IR066SIT> c066Sit2 = consulta2(IR066SIT.class,
					" (DatApu = :dDat1 OR DatApu = :dDat2 OR DatApu = :dDat3)  And CodSit IN (1,51, 307, 308, 309, 357, 358, 409) And NumEmp = :iNumEmp And TipCol = :iTipCol And NumCad = :iNumCad ",
					"DatApu", paramProvider4);

			try {
				IR066SIT ir066sit = c066Sit.newBuffer();
				while (c066Sit2.next()) {
					c066Sit2.read(ir066sit);
					if (dDatAux.equals(LocalDate.parse("2018-01-01"))
							|| (dDatAux.isBefore(ir066sit.getDatApu()) || dDatAux.isAfter(ir066sit.getDatApu()))) {
						iDiaTra++;
						dDatAux = ir066sit.getDatApu();
					}
				}
			} finally {
				c066Sit2.close();
			}
			if (iDiaTra == 3) {
				try {
					apuracao.criarAlerta(17);
				} catch (ApuracaoException e) {

				}
			}
		}

		paramProvider.setParam("iCodSin", iCodSin);
		IR014SIN c014Sin = consulta(IR014SIN.class, "CodSin=:iCodSin", paramProvider);
		if (c014Sin != null) {
			iIntMin4h = c014Sin.getInMiJE();
			iIntMin6h = c014Sin.getInMiJA();
			iCodDsi = c014Sin.getCodDSi();
		}

		if (iCodDsi == 0) {
			int iNumEmp = colaborador.getNumEmp();
			paramProvider.setParam("iNumEmp", iNumEmp);
			IR030EMP c030Emp = consulta(IR030EMP.class, "NumEmp=:iNumEmp", paramProvider);
			if (c030Emp != null) {
				iCodDsi = c030Emp.getCodDSi();
			}
		}

		iSitTra = apuracao.getDefinicaoSituacoes().getSituacaoTrabalho();
		iSitAdi = apuracao.getDefinicaoSituacoes().getSituacaoAdicionalNoturno();
		iSitNId = apuracao.getDefinicaoSituacoes().getSituacaoMarcacoesInvalidas();
		iNorAnt = apuracao.getDefinicaoSituacoes().getSituacaoExtrasAntes();
		iNorFer = apuracao.getDefinicaoSituacoes().getSituacaoExtrasFeriado();
		iSitVIn = apuracao.getDefinicaoSituacoes().getSituacaoViolacaoIntrajornada();
		iSitFal = apuracao.getDefinicaoSituacoes().getSituacaoFalta();

		if (marcacaoInvalida(apuracao)) {
			apuracao.setHorSit(iSitNId, 480);
		}

		iHorTra = apuracao.getHorasTrabalhadas().getHorasDiurnas() + apuracao.getHorasTrabalhadas().getHorasNoturnas();
		// iHorRef =
		// apuracao.getIntervaloCalculado(apuracao.getNumeroIntervaloRefeicao()).getIntervaloMinutos();

		List<MarcacaoRegra> marcacaoAtualList = apuracao.getMarcacoesRealizadas(true); 
		if (marcacaoAtualList.size() >= 4) {
			for (int i = 0; i < marcacaoAtualList.size(); i++) {
				if ((marcacaoAtualList.get(i).getUso().getUso() == 2) && (!bEntrou))
					bEntrou = true;
				else if ((marcacaoAtualList.get(i).getUso().getUso() == 2) && (bEntrou)) {
					if (i == marcacaoAtualList.size() - 1) {
						iBatInt = 0;
						break;
					} else
						break;
				}
				iBatInt++;
			}
		}

		for (int i = 0; i < marcacaoAtualList.size(); i++) {
			if (marcacaoAtualList.get(i).getUso().getUso() == 21)
				bTem21 = true;
		}

		int nHorIniMot = 0;
		int nHorFimMot = 0;
		// Apenas para motoristas
		if (bTem21) {
			MappedParamProvider paramProvider4 = new MappedParamProvider();
			paramProvider4.setParam("dDatApu", apuracao.getData());
			paramProvider4.setParam("iNumEmp", colaborador.getNumEmp());
			paramProvider4.setParam("iTipCol", colaborador.getTipCol());
			paramProvider4.setParam("iNumCad", colaborador.getNumCad());
			ICursor<E_R070ACC> c070Acc = consulta2(E_R070ACC.class,
					"DatApu = :dDatApu And NumEmp = :iNumEmp And TipCol = :iTipCol And NumCad = :iNumCad ", "HorAcc",
					paramProvider4);
			try {
				E_R070ACC ir070acc = c070Acc.newBuffer();
				while (c070Acc.next()) {
					c070Acc.read(ir070acc);
					if (!ir070acc.isUSU_NumMacNull()) {
						if ((nHorIniMot != 0) && (nHorFimMot != 0) && (nHorFimMot - nHorIniMot >= 60)){
							break;
						}
						else if (ir070acc.getUSU_NumMac() == 5) {
							nHorIniMot = ir070acc.getHorAcc();
						} else if (ir070acc.getUSU_NumMac() == 8) {
							nHorFimMot = ir070acc.getHorAcc();
						}
					}
				}
			} finally {
				c070Acc.close();
			}
			if ((nHorIniMot != 0) && (nHorFimMot != 0))
				iHorRef = nHorFimMot - nHorIniMot;

		}

		if ((marcacaoAtualList.size() >= 6) && (iBatInt > 0) && (bTem21) && (iHorRef == 0)) {
			if (marcacaoAtualList.get(iBatInt).getHora() > marcacaoAtualList.get(iBatInt + 1).getHora()) {
				iHorRef = marcacaoAtualList.get(iBatInt + 1).getHora()
						- (1440 - marcacaoAtualList.get(iBatInt).getHora());
			} else {
				iHorRef = marcacaoAtualList.get(iBatInt + 1).getHora() - marcacaoAtualList.get(iBatInt).getHora();
			}
			/*
			 * if (marcacaoAtualList.size() > 5) { int iHorRefAux =
			 * marcacaoAtualList.get(4).getHora() -
			 * marcacaoAtualList.get(3).getHora(); if (iHorRefAux > iHorRef)
			 * iHorRef = iHorRefAux; }
			 */
		} else if ((marcacaoAtualList.size() == 4) && (!bTem21)) {
			if (marcacaoAtualList.get(1).getHora() > marcacaoAtualList.get(2).getHora()) {
				iHorRef = marcacaoAtualList.get(2).getHora() - (1440 - marcacaoAtualList.get(1).getHora());
			} else {
				iHorRef = marcacaoAtualList.get(2).getHora() - marcacaoAtualList.get(1).getHora();
			}
			if (marcacaoAtualList.size() > 6) {
				int iHorRefAux = marcacaoAtualList.get(iBatInt + 1).getHora()
						- marcacaoAtualList.get(iBatInt).getHora();
				if (iHorRefAux > iHorRef)
					iHorRef = iHorRefAux;
			}
		}

		if (iHorRef == 0) {
			iHorRef = apuracao.getHorasSeparadas(TipoIntervalo.REFEICAO).getHorasDiurnas()
					+ apuracao.getHorasSeparadas(TipoIntervalo.REFEICAO).getHorasNoturnas();
		}

		if (iHorRef > 120) {
			try {
				apuracao.criarAlerta(14);
			} catch (ApuracaoException e) {
			}
		}

		if (iIntMin4h > 0 && iIntMin6h > 0 && apuracao.getMarcacoesRealizadas(true).size() > 0) {
			if (iHorTra >= 240 && iHorTra <= 360 && iHorRef < iIntMin4h) {
				iHorInt = iHorRef - iIntMin4h;
				if (iHorInt < 0)
					iHorInt = iHorInt * -1;
				apuracao.setHorSit(iSitVIn, iHorInt);
			} else if (iHorTra > 360 && iHorRef < iIntMin6h) {
				iHorInt = iHorRef - iIntMin6h;
				if (iHorInt < 0)
					iHorInt = iHorInt * -1;
				apuracao.setHorSit(iSitVIn, iHorInt);
			}
		}
		
		ICursor<IR010TOB> c010Tob = consulta2(IR010TOB.class, "CodTot = 3 ", "", paramProvider);
		try {
			IR010TOB ir010Tob = c010Tob.newBuffer();
			while (c010Tob.next()) {
				c010Tob.read(ir010Tob);
				iTotTob = iTotTob + apuracao.getHorSit(ir010Tob.getCodSit());
			}
		} finally {
			c010Tob.close();
		}
		apuracao.setHorSit(iSitAdi, iTotTob + apuracao.getHorSit(iSitAdi));

		if (lHorDsr == 0) {
			int iCodEsc = apuracao.getHistoricoEscala().getCodEsc();
			MappedParamProvider paramProvider5 = new MappedParamProvider();
			paramProvider5.setParam("iCodEsc", iCodEsc);
			ICursor<IR006ESC> c006Esc = consulta2(IR006ESC.class, " CodEsc = :iCodEsc ", "", paramProvider5);
			try {
				IR006ESC ir006Esc = c006Esc.newBuffer();
				while (c006Esc.next()) {
					c006Esc.read(ir006Esc);
					lHorDsr = ir006Esc.getHorDsr();
				}
			} finally {
				c006Esc.close();
			}
		}

		List<HistoricoAfastamento> listaAfastamentos = apuracao.getHistoricosAfastamento();
		List<Compensacao> compensacoes = apuracao.getCompensacoes(apuracao.getData());

		if (apuracao.getHorSit(iSitNId) == 0 && compensacoes.size() == 0 && listaAfastamentos.size() == 0) {
			if ((apuracao.getMarcacoesRealizadas(true).size() >= 4 && apuracao.getMarcacoesRealizadas(true).size() <= 8)
					|| (apuracao.getHorario().getCodigo() < 9996)) {

				MappedParamProvider paramProvider2 = new MappedParamProvider();
				paramProvider2.setParam("iSitTra", iSitTra);
				paramProvider2.setParam("iNorAnt", iNorAnt);
				paramProvider2.setParam("iNorFer", iNorFer);
				ICursor<IR010SIT> c010Sit = consulta2(IR010SIT.class,
						"CodSit = :iNorAnt OR CodSit = :iSitTra OR CodSit = :iNorFer ", "", paramProvider2);
				try {
					IR010SIT ir010Sit = c010Sit.newBuffer();
					while (c010Sit.next()) {
						c010Sit.read(ir010Sit);
						if (ir010Sit.getCodSit() == iSitTra)
							iSitTraCmp = ir010Sit.getSitCmp();
						else if (ir010Sit.getCodSit() == iNorAnt)
							iNorAntCmp = ir010Sit.getSitCmp();
					}
				} finally {
					c010Sit.close();
				}

				Double dAdNot = iHorReaNot * 1.142857;
				Math.floor(dAdNot);
				int iHorCon = iHorReaDiu + dAdNot.intValue();

				if (iFatRed > 0 && ((iCodSin != 3) && (iCodSin != 5))) {
					if (iHorCon > iFatRed) {
						apuracao.setHorSit(iSitTra, iHorReaDiu);
						apuracao.setHorSit(iSitTraCmp, iHorReaNot);
						if (apuracao.getHorasSeparadas(TipoIntervalo.EXTRA).getHorasNoturnas() > 0) {
							if ((iHorCon - iFatRed - apuracao.getHorSit(iNorAnt)) > 0) {
								// apuracao.setHorSit(iNorAntCmp, iHorCon -
								// iFatRed - apuracao.getHorSit(iNorAnt));
							} else
								apuracao.zeraHorasSituacao(iNorAntCmp);
						} else {
							apuracao.setHorSit(iNorAnt, iHorCon - iFatRed);
							apuracao.setHorSit(iSitTra, apuracao.getHorSit(iSitTra) - apuracao.getHorSit(iNorAnt));
						}
					} else {

						apuracao.setHorSit(iSitFal, iFatRed - iHorCon);
						apuracao.setHorSit(iSitTraCmp, iHorReaNot);
					}
				} else if (iFatRed > 0 && ((iCodSin == 3) || (iCodSin == 5))) {
					if (iHorCon > iFatRed) {
						apuracao.setHorSit(iSitTra, iHorReaDiu);
						apuracao.setHorSit(iSitTraCmp, iHorReaNot);
						iTotExt = iHorCon - iFatRed;

						if ((apuracao.getHorSit(302) + apuracao.getHorSit(352)) == 120) {
							Double dVarAux = apuracao.getHorSit(352) * 1.142857;
							Math.floor(dVarAux);
							iHExt60 = (dVarAux.intValue() - apuracao.getHorSit(352));
							dVarAux = apuracao.getHorSit(353) * 1.142857;
							Math.floor(dVarAux);
							apuracao.setHorSit(353, iHExt60 + dVarAux.intValue());
							if (apuracao.getHorSit(303) > 0) {
								iTotExt = iTotExt - 120;
								apuracao.setHorSit(303, iTotExt - apuracao.getHorSit(353));
							}
						} else if (apuracao.getHorSit(302) == 0 && apuracao.getHorSit(352) > 0) {
							apuracao.setHorSit(352, iTotExt);
						} else if (apuracao.getHorSit(302) > 0 && apuracao.getHorSit(352) == 0) {
							apuracao.setHorSit(302, iTotExt);
						}
					} else if (marcacaoAtualList.size() == 0) {
						apuracao.setHorSit(iSitFal, iFatRed - iHorCon);
						apuracao.setHorSit(iSitTraCmp, apuracao.getHorSit(iSitTra) - apuracao.getHorSit(iNorAnt));
					}
				}
			}

			if (lHorDsr == 0) {
				int iCodEsc = apuracao.getHistoricoEscala().getCodEsc();
				MappedParamProvider paramProvider5 = new MappedParamProvider();
				paramProvider5.setParam("iCodEsc", iCodEsc);
				ICursor<IR006ESC> c006Esc = consulta2(IR006ESC.class, " CodEsc = :iCodEsc ", "", paramProvider5);
				try {
					IR006ESC ir006Esc = c006Esc.newBuffer();
					while (c006Esc.next()) {
						c006Esc.read(ir006Esc);
						lHorDsr = ir006Esc.getHorDsr();
					}
				} finally {
					c006Esc.close();
				}
			}

			if (((iHorPreDiu + iHorPreNot) > (iHorReaDiu + iHorReaNot)) && (compensacoes.size() == 0)
					&& (listaAfastamentos.size() == 0) && (apuracao.getMarcacoesRealizadas(true).size() == 0)) {
				apuracao.setHorSit(15, (int) lHorDsr);
				apuracao.zeraHorasSituacao(iSitFal);
				apuracao.zeraHorasSituacao(115);
			}
		if( colaborador.getNumCad() == 73222222){//removido frabicia
			//Início tratamento Atualização Senior escala permanente
			if(marcacaoAtualList.size() == 4 || marcacaoAtualList.size() == 6 || marcacaoAtualList.size() == 8){
				//Início tratamento Atualização Senior escala permanente
				int minutosTrabalhados = 0;
				int minutosDiurnos = 0;
				int minutosNoturnos = 0;
				int minutosExtrasDiurnos = 0;
				int minutosExtrasNoturnos = 0;
	
				int inicioNoturno = 1320; // 22:00 em minutos
				int fimNoturno = 300; // 05:00 em minutos
				int limiteTrabalho = 440; // 7 horas e 20 minutos em minutos, validar com escala
	
			      for (int i = 0; i < marcacaoAtualList.size(); i += 2) {
			            int entrada = marcacaoAtualList.get(i).getHora();
			            int saida = marcacaoAtualList.get(i + 1).getHora();

			            if (saida < entrada) {
			                saida += 1440; // Adiciona 24 horas em minutos
			            }

			            // Calcula a duração total do período trabalhado
			            int duracaoTotal = saida - entrada;

			            int minutosNoturnosNoInicio = Math.max(Math.min(saida, fimNoturno) - entrada, 0);

			            int minutosNoturnosNoFim = Math.max(saida - Math.max(entrada, inicioNoturno), 0);

			            int minutosNoturnosNoPeriodo = minutosNoturnosNoInicio + minutosNoturnosNoFim;

			            int minutosDiurnosNoPeriodo = duracaoTotal - minutosNoturnosNoPeriodo;


			            if (minutosTrabalhados + duracaoTotal <= limiteTrabalho) {
			                minutosTrabalhados += duracaoTotal;
			                minutosDiurnos += minutosDiurnosNoPeriodo;
			                minutosNoturnos += minutosNoturnosNoPeriodo;
			            } else {
			                // Se já ultrapassou os 440 minutos, calcular as horas extras
			                int minutosRestantes = limiteTrabalho - minutosTrabalhados;

			                if (minutosRestantes > 0) {

			                    if (minutosDiurnosNoPeriodo >= minutosRestantes) {
			                        minutosDiurnos += minutosRestantes;
			                        minutosDiurnosNoPeriodo -= minutosRestantes;
			                    } else {
			                        minutosDiurnos += minutosDiurnosNoPeriodo;
			                        minutosRestantes -= minutosDiurnosNoPeriodo;
			                        minutosNoturnos += minutosRestantes;
			                        minutosNoturnosNoPeriodo -= minutosRestantes;
			                    }

			                    minutosTrabalhados = limiteTrabalho; 
			                }

			                if (minutosDiurnosNoPeriodo > 0) {
			                    minutosExtrasDiurnos += minutosDiurnosNoPeriodo;
			                }
			                if (minutosNoturnosNoPeriodo > 0) {
			                    minutosExtrasNoturnos += minutosNoturnosNoPeriodo;
			                }
			            }
			        }
				
				apuracao.zeraHorasSituacao(1, 51, 301, 312,302,352);
				if(minutosDiurnos > 0)
					apuracao.setHorSit(1,minutosDiurnos);       //Sit Trabalhan
				
				if(minutosNoturnos > 0)
					apuracao.setHorSit(51,minutosNoturnos);     //Sit Trab Not
				
				
				if(minutosExtrasDiurnos > 120){
					apuracao.setHorSit(301,120); //Sit extras diurnas 50%
					apuracao.setHorSit(312,minutosExtrasDiurnos - 120); //Sit extras diurnas 60%
				} else {
					if(minutosExtrasDiurnos > 0){
						apuracao.setHorSit(301,minutosExtrasDiurnos);	
					} 
				}
				if(minutosExtrasNoturnos > 120){
					apuracao.setHorSit(302,120); //Sit extras Noturnas 50%
					apuracao.setHorSit(352,minutosExtrasNoturnos - 120); //Sit extras Noturnas 60%
				} else {
					if(minutosExtrasNoturnos > 0){
						apuracao.setHorSit(302,minutosExtrasNoturnos);	
					} 			
				}				
			}
		}
	}
		if (apuracao.getHorario().getCodigo() == 9997) {
			apuracao.zeraHorasSituacao(15, 101, 103, 105);
		}

		// INTRAJORNADA
		if (apuracao.getHorSit(50) > 0) {
			if ((((iHorPreDiu + iHorPreNot) >= 360) && ((iHorReaDiu + iHorReaNot) < 360))) {
				if (apuracao.getHorSit(50) <= 15) {
					try {
						apuracao.criarAlerta(15);
					} catch (ApuracaoException e) {
						e.printStackTrace();
					}
				}
			} else {
				try {
					apuracao.criarAlerta(15);
				} catch (ApuracaoException e) {
					e.printStackTrace();
				}
			}
		}

		int iConBat = 0;
		int iNumEmp = apuracao.getColaborador().getNumEmp();
		int iTipCol = apuracao.getColaborador().getTipCol();
		int iNumCad = apuracao.getColaborador().getNumCad();
		MappedParamProvider paramProvider6 = new MappedParamProvider();
		paramProvider6.setParam("iNumEmp", iNumEmp);
		paramProvider6.setParam("iTipCol", iTipCol);
		paramProvider6.setParam("iNumCad", iNumCad);
		paramProvider6.setParam("dDatApu", apuracao.getData());
		ICursor<IR070ACC> c070Acc = consulta2(IR070ACC.class,
				" NumEmp = :iNumEmp And TipCol = :iTipCol And NumCad = :iNumCad And DatApu = :dDatApu And OriAcc = 'E'  ",
				"", paramProvider6);
		try {
			IR070ACC ir070Acc = c070Acc.newBuffer();
			while (c070Acc.next()) {
				c070Acc.read(ir070Acc);
				iConBat++;
			}
		} finally {
			c070Acc.close();
		}

		if ((iConBat % 2) != 0) {
			try {
				apuracao.criarAlerta(1);
			} catch (ApuracaoException e) {
				e.printStackTrace();
			}
		}
		//Ajuste chamado 222316 solicitação Emerson.
		//if( colaborador.getNumCad() == 11292){
			//int iHorCalc = (int) Math.floor(apuracao.getHorSit(51) * 1.1428);//Feito assim pois a situação 51 não estava sendo calculada antes
			//iHorPreNot = (int) Math.floor(iHorPreNot * 1.1428);//Na escala não vem convertido.
			//if((apuracao.getHorSit(1) + iHorCalc + apuracao.getHorSit(301) + apuracao.getHorSit(302)+ apuracao.getHorSit(351) + apuracao.getHorSit(352)) >= (iHorPreDiu + iHorPreNot)){
			//	if(apuracao.getHorSit(115)>0){
			//		apuracao.zeraHorasSituacao(115);
			//	}
				//apuracao.setHorSit(115,iHorPreNot);
			//}
		
		//Fim Ajuste
	}

	// Método Genérico para fazer um cursor
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

	// Método Genérico para fazer um cursor
	private <C> ICursor<C> consulta2(Class<C> entidade, String filtro, String sOrdenacao,
			MappedParamProvider paramProvider) {
		ICursor<C> cursor = getContainer().getEntitySession().newCursor(entidade);
		if (filtro != "")
			cursor.addFilter(filtro, paramProvider);
		if (sOrdenacao != "")
			cursor.setOrder(sOrdenacao);
		cursor.open();
		try {
			return cursor;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Passar como parâmetro o contexto de apuração
	 * 
	 * @param ContextoApuracao
	 * @return boolean
	 */
	private boolean marcacaoInvalida(ContextoApuracao apuracao) {
		if ((apuracao.getMarcacoesRealizadas(true).size() % 2) == 1)
			return true;
		else
			return false;
	}

}
